package com.innercircle.sacco.investment.service;

import com.innercircle.sacco.common.event.InvestmentActivatedEvent;
import com.innercircle.sacco.common.event.InvestmentApprovedEvent;
import com.innercircle.sacco.common.event.InvestmentCreatedEvent;
import com.innercircle.sacco.common.event.InvestmentDisposedEvent;
import com.innercircle.sacco.common.event.InvestmentIncomeRecordedEvent;
import com.innercircle.sacco.common.event.InvestmentRejectedEvent;
import com.innercircle.sacco.common.event.InvestmentRolledOverEvent;
import com.innercircle.sacco.common.event.InvestmentValuationRecordedEvent;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.common.outbox.EventOutboxWriter;
import com.innercircle.sacco.common.util.SecureIdGenerator;
import com.innercircle.sacco.investment.dto.CreateInvestmentRequest;
import com.innercircle.sacco.investment.dto.DisposeInvestmentRequest;
import com.innercircle.sacco.investment.dto.InvestmentSummaryResponse;
import com.innercircle.sacco.investment.dto.RecordIncomeRequest;
import com.innercircle.sacco.investment.dto.RecordValuationRequest;
import com.innercircle.sacco.investment.dto.RollOverRequest;
import com.innercircle.sacco.investment.entity.DisposalType;
import com.innercircle.sacco.investment.entity.Investment;
import com.innercircle.sacco.investment.entity.InvestmentIncome;
import com.innercircle.sacco.investment.entity.InvestmentStatus;
import com.innercircle.sacco.investment.entity.InvestmentType;
import com.innercircle.sacco.investment.entity.InvestmentValuation;
import com.innercircle.sacco.investment.guard.InvestmentTransitionGuards;
import com.innercircle.sacco.investment.repository.InvestmentIncomeRepository;
import com.innercircle.sacco.investment.repository.InvestmentRepository;
import com.innercircle.sacco.investment.repository.InvestmentValuationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvestmentServiceImpl implements InvestmentService {

    private static final int MAX_REFERENCE_GENERATION_ATTEMPTS = 5;
    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final EnumSet<InvestmentType> UNIT_TYPES = EnumSet.of(InvestmentType.UNIT_TRUST, InvestmentType.EQUITY);
    private static final EnumSet<InvestmentStatus> OPEN_PORTFOLIO_STATUSES =
            EnumSet.of(InvestmentStatus.ACTIVE, InvestmentStatus.PARTIALLY_DISPOSED, InvestmentStatus.ROLLED_OVER, InvestmentStatus.MATURED);
    private static final EnumSet<InvestmentStatus> INCOME_ALLOWED_STATUSES =
            EnumSet.of(InvestmentStatus.ACTIVE, InvestmentStatus.PARTIALLY_DISPOSED, InvestmentStatus.ROLLED_OVER, InvestmentStatus.MATURED);
    private static final EnumSet<InvestmentStatus> VALUATION_ALLOWED_STATUSES =
            EnumSet.of(InvestmentStatus.ACTIVE, InvestmentStatus.PARTIALLY_DISPOSED, InvestmentStatus.ROLLED_OVER, InvestmentStatus.MATURED);

    private final InvestmentRepository investmentRepository;
    private final InvestmentIncomeRepository investmentIncomeRepository;
    private final InvestmentValuationRepository investmentValuationRepository;
    private final EventOutboxWriter outboxWriter;

    @Override
    @Transactional(readOnly = true)
    public List<Investment> listInvestments() {
        return investmentRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Investment getInvestment(UUID investmentId) {
        return findInvestment(investmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public InvestmentSummaryResponse getSummary() {
        List<Investment> investments = listInvestments();

        List<Investment> openPortfolio = investments.stream()
                .filter(investment -> OPEN_PORTFOLIO_STATUSES.contains(investment.getStatus()))
                .toList();

        BigDecimal totalInvested = sumMoney(openPortfolio.stream().map(Investment::getPurchasePrice).toList());
        BigDecimal currentValue = sumMoney(openPortfolio.stream().map(Investment::getCurrentValue).toList());
        BigDecimal unrealisedGain = money(totalInvested.subtract(currentValue).negate());

        LocalDate today = LocalDate.now();
        LocalDate yearStart = LocalDate.of(today.getYear(), 1, 1);
        BigDecimal incomeYtd = money(investmentIncomeRepository.sumByIncomeDateBetween(yearStart, today));

        Map<InvestmentType, BigDecimal> typeTotals = new EnumMap<>(InvestmentType.class);
        for (Investment investment : openPortfolio) {
            BigDecimal amount = money(investment.getCurrentValue());
            typeTotals.merge(investment.getInvestmentType(), amount, BigDecimal::add);
        }

        List<InvestmentSummaryResponse.InvestmentTypeAllocation> byType = typeTotals.entrySet().stream()
                .sorted(Map.Entry.<InvestmentType, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                .map(entry -> new InvestmentSummaryResponse.InvestmentTypeAllocation(
                        entry.getKey(),
                        money(entry.getValue()),
                        percentage(entry.getValue(), currentValue)
                ))
                .toList();

        return new InvestmentSummaryResponse(
                totalInvested,
                currentValue,
                unrealisedGain,
                incomeYtd,
                investments.stream().filter(i -> EnumSet.of(InvestmentStatus.ACTIVE, InvestmentStatus.PARTIALLY_DISPOSED, InvestmentStatus.ROLLED_OVER).contains(i.getStatus())).count(),
                investments.stream().filter(i -> i.getStatus() == InvestmentStatus.MATURED).count(),
                investments.stream().filter(i -> i.getStatus() == InvestmentStatus.PROPOSED).count(),
                investments.stream().filter(i -> i.getStatus() == InvestmentStatus.CLOSED).count(),
                byType
        );
    }

    @Override
    @Transactional
    public Investment createInvestment(CreateInvestmentRequest request, String actor) {
        validateDatesForCreate(request.purchaseDate(), request.maturityDate());
        validateUnitFieldsForCreate(request.investmentType(), request.units(), request.navPerUnit());

        BigDecimal purchasePrice = money(request.purchasePrice());
        BigDecimal units = request.units() != null ? units(request.units()) : null;
        BigDecimal navPerUnit = request.navPerUnit() != null ? nav(request.navPerUnit()) : null;

        Investment investment = new Investment(
                generateUniqueReferenceNumber(),
                request.name().trim(),
                request.investmentType(),
                request.institution().trim(),
                money(request.faceValue()),
                purchasePrice,
                initialCurrentValue(request.investmentType(), purchasePrice, units, navPerUnit),
                rate(request.interestRate()),
                request.purchaseDate(),
                request.maturityDate(),
                units,
                navPerUnit,
                trimmedOrNull(request.notes())
        );
        investment.setCreatedBy(actor);

        Investment saved = investmentRepository.save(investment);

        outboxWriter.write(new InvestmentCreatedEvent(
                saved.getId(),
                saved.getReferenceNumber(),
                saved.getInvestmentType().name(),
                saved.getPurchasePrice(),
                saved.getCurrentValue(),
                UUID.randomUUID(),
                actor
        ), "Investment", saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public Investment approveInvestment(UUID investmentId, String actor) {
        Investment investment = findInvestment(investmentId);

        InvestmentTransitionGuards.INVESTMENT.validate(investment.getStatus(), InvestmentStatus.APPROVED);

        investment.setStatus(InvestmentStatus.APPROVED);
        investment.setApprovedBy(actor);
        investment.setApprovedAt(Instant.now());
        investment.setRejectedBy(null);
        investment.setRejectedAt(null);
        investment.setRejectionReason(null);

        Investment saved = investmentRepository.save(investment);

        outboxWriter.write(new InvestmentApprovedEvent(
                saved.getId(),
                saved.getReferenceNumber(),
                UUID.randomUUID(),
                actor
        ), "Investment", saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public Investment rejectInvestment(UUID investmentId, String reason, String actor) {
        Investment investment = findInvestment(investmentId);

        InvestmentTransitionGuards.INVESTMENT.validate(investment.getStatus(), InvestmentStatus.REJECTED);

        String trimmedReason = trimmedOrNull(reason);

        investment.setStatus(InvestmentStatus.REJECTED);
        investment.setRejectedBy(actor);
        investment.setRejectedAt(Instant.now());
        investment.setRejectionReason(trimmedReason);
        if (trimmedReason != null) {
            investment.setNotes(mergeNotes(investment.getNotes(), "Rejection reason: " + trimmedReason));
        }

        Investment saved = investmentRepository.save(investment);

        outboxWriter.write(new InvestmentRejectedEvent(
                saved.getId(),
                saved.getReferenceNumber(),
                trimmedReason,
                UUID.randomUUID(),
                actor
        ), "Investment", saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public Investment activateInvestment(UUID investmentId, String actor) {
        Investment investment = findInvestment(investmentId);

        InvestmentTransitionGuards.INVESTMENT.validate(investment.getStatus(), InvestmentStatus.ACTIVE);

        investment.setStatus(InvestmentStatus.ACTIVE);
        Investment saved = investmentRepository.save(investment);

        outboxWriter.write(new InvestmentActivatedEvent(
                saved.getId(),
                saved.getReferenceNumber(),
                saved.getPurchasePrice(),
                UUID.randomUUID(),
                actor
        ), "Investment", saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public Investment disposeInvestment(UUID investmentId, DisposeInvestmentRequest request, String actor) {
        Investment investment = findInvestment(investmentId);

        BigDecimal proceedsAmount = money(request.proceedsAmount());
        BigDecimal fees = money(request.fees());

        if (fees.compareTo(proceedsAmount) > 0) {
            throw new BusinessException("Fees cannot exceed disposal proceeds");
        }

        InvestmentStatus targetStatus = resolveDisposalTargetStatus(investment, request.disposalType(), proceedsAmount, request.unitsRedeemed());
        InvestmentTransitionGuards.INVESTMENT.validate(investment.getStatus(), targetStatus);

        applyDisposal(investment, request, targetStatus, proceedsAmount, fees);

        if (request.notes() != null && !request.notes().isBlank()) {
            investment.setNotes(mergeNotes(investment.getNotes(), request.notes().trim()));
        }

        Investment saved = investmentRepository.save(investment);

        outboxWriter.write(new InvestmentDisposedEvent(
                saved.getId(),
                saved.getReferenceNumber(),
                request.disposalType().name(),
                proceedsAmount,
                fees,
                UUID.randomUUID(),
                actor
        ), "Investment", saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public Investment rollOverInvestment(UUID investmentId, RollOverRequest request, String actor) {
        Investment investment = findInvestment(investmentId);

        if (!request.newMaturityDate().isAfter(investment.getPurchaseDate())) {
            throw new BusinessException("New maturity date must be after purchase date");
        }

        InvestmentTransitionGuards.INVESTMENT.validate(investment.getStatus(), InvestmentStatus.ROLLED_OVER);

        LocalDate previousMaturityDate = investment.getMaturityDate();
        investment.setMaturityDate(request.newMaturityDate());
        investment.setInterestRate(rate(request.newInterestRate()));
        investment.setStatus(InvestmentStatus.ROLLED_OVER);
        if (request.notes() != null && !request.notes().isBlank()) {
            investment.setNotes(mergeNotes(investment.getNotes(), request.notes().trim()));
        }

        Investment saved = investmentRepository.save(investment);

        outboxWriter.write(new InvestmentRolledOverEvent(
                saved.getId(),
                saved.getReferenceNumber(),
                previousMaturityDate,
                saved.getMaturityDate(),
                saved.getInterestRate(),
                UUID.randomUUID(),
                actor
        ), "Investment", saved.getId());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvestmentIncome> getIncomeHistory(UUID investmentId) {
        findInvestment(investmentId);
        return investmentIncomeRepository.findByInvestmentIdOrderByIncomeDateDescCreatedAtDesc(investmentId);
    }

    @Override
    @Transactional
    public InvestmentIncome recordIncome(UUID investmentId, RecordIncomeRequest request, String actor) {
        Investment investment = findInvestment(investmentId);

        if (!INCOME_ALLOWED_STATUSES.contains(investment.getStatus())) {
            throw new BusinessException("Income can only be recorded for active, partially disposed, rolled over, or matured investments");
        }

        InvestmentIncome income = new InvestmentIncome(
                investmentId,
                request.incomeType(),
                money(request.amount()),
                request.incomeDate(),
                trimmedOrNull(request.referenceNumber()),
                trimmedOrNull(request.notes())
        );
        income.setCreatedBy(actor);

        InvestmentIncome saved = investmentIncomeRepository.save(income);

        // Reflect realized income in portfolio value so holdings table updates Current Value/Return.
        BigDecimal updatedCurrentValue = money(money(investment.getCurrentValue()).add(saved.getAmount()));
        investment.setCurrentValue(updatedCurrentValue);
        if (isUnitType(investment.getInvestmentType())
                && investment.getUnits() != null
                && investment.getUnits().compareTo(BigDecimal.ZERO) > 0) {
            investment.setNavPerUnit(updatedCurrentValue.divide(investment.getUnits(), 4, RoundingMode.HALF_UP));
        }
        investmentRepository.save(investment);

        outboxWriter.write(new InvestmentIncomeRecordedEvent(
                investment.getId(),
                saved.getId(),
                saved.getIncomeType().name(),
                saved.getAmount(),
                saved.getReferenceNumber(),
                UUID.randomUUID(),
                actor
        ), "Investment", investment.getId());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvestmentValuation> getValuationHistory(UUID investmentId) {
        findInvestment(investmentId);
        return investmentValuationRepository.findByInvestmentIdOrderByValuationDateDescCreatedAtDesc(investmentId);
    }

    @Override
    @Transactional
    public InvestmentValuation recordValuation(UUID investmentId, RecordValuationRequest request, String actor) {
        Investment investment = findInvestment(investmentId);

        if (!VALUATION_ALLOWED_STATUSES.contains(investment.getStatus())) {
            throw new BusinessException("Valuation can only be recorded for active, partially disposed, rolled over, or matured investments");
        }

        if (request.valuationDate().isBefore(investment.getPurchaseDate())) {
            throw new BusinessException("Valuation date cannot be before purchase date");
        }

        boolean unitType = isUnitType(investment.getInvestmentType());
        BigDecimal navPerUnit = request.navPerUnit() != null ? nav(request.navPerUnit()) : null;
        BigDecimal marketValue;

        if (unitType) {
            if (navPerUnit == null && request.marketValue() == null) {
                throw new BusinessException("Provide NAV per unit or market value for this investment type");
            }
            if (navPerUnit != null) {
                if (investment.getUnits() == null || investment.getUnits().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException("Units held must be available to derive valuation from NAV");
                }
                marketValue = money(investment.getUnits().multiply(navPerUnit));
            } else {
                marketValue = money(request.marketValue());
            }
        } else {
            if (request.marketValue() == null) {
                throw new BusinessException("Market value is required for this investment type");
            }
            marketValue = money(request.marketValue());
            navPerUnit = null;
        }

        InvestmentValuation valuation = new InvestmentValuation(
                investmentId,
                marketValue,
                navPerUnit,
                request.valuationDate(),
                request.source().trim()
        );
        valuation.setCreatedBy(actor);

        InvestmentValuation savedValuation = investmentValuationRepository.save(valuation);

        investment.setCurrentValue(marketValue);
        if (navPerUnit != null) {
            investment.setNavPerUnit(navPerUnit);
        }
        investmentRepository.save(investment);

        outboxWriter.write(new InvestmentValuationRecordedEvent(
                investment.getId(),
                savedValuation.getId(),
                savedValuation.getMarketValue(),
                savedValuation.getNavPerUnit(),
                savedValuation.getValuationDate(),
                UUID.randomUUID(),
                actor
        ), "Investment", investment.getId());

        return savedValuation;
    }

    private Investment findInvestment(UUID investmentId) {
        return investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment", investmentId));
    }

    private void validateDatesForCreate(LocalDate purchaseDate, LocalDate maturityDate) {
        if (maturityDate != null && maturityDate.isBefore(purchaseDate)) {
            throw new BusinessException("Maturity date cannot be before purchase date");
        }
    }

    private void validateUnitFieldsForCreate(InvestmentType investmentType, BigDecimal units, BigDecimal navPerUnit) {
        boolean unitType = isUnitType(investmentType);

        if (unitType) {
            if (units == null || navPerUnit == null) {
                throw new BusinessException("Units and NAV per unit are required for unit trust and equity investments");
            }
            return;
        }

        if (units != null || navPerUnit != null) {
            throw new BusinessException("Units and NAV per unit are only allowed for unit trust and equity investments");
        }
    }

    private BigDecimal initialCurrentValue(InvestmentType investmentType,
                                           BigDecimal purchasePrice,
                                           BigDecimal units,
                                           BigDecimal navPerUnit) {
        if (!isUnitType(investmentType) || units == null || navPerUnit == null) {
            return money(purchasePrice);
        }
        return money(units.multiply(navPerUnit));
    }

    private InvestmentStatus resolveDisposalTargetStatus(Investment investment,
                                                         DisposalType disposalType,
                                                         BigDecimal proceedsAmount,
                                                         BigDecimal unitsRedeemed) {
        return switch (disposalType) {
            case FULL -> InvestmentStatus.CLOSED;
            case MATURITY -> InvestmentStatus.MATURED;
            case PARTIAL -> {
                if (isUnitType(investment.getInvestmentType()) && investment.getUnits() != null) {
                    if (unitsRedeemed == null) {
                        throw new BusinessException("Units redeemed are required for partial disposal of unit investments");
                    }
                    BigDecimal redeemed = units(unitsRedeemed);
                    if (redeemed.compareTo(investment.getUnits()) > 0) {
                        throw new BusinessException("Units redeemed cannot exceed units held");
                    }
                    yield redeemed.compareTo(investment.getUnits()) == 0
                            ? InvestmentStatus.CLOSED
                            : InvestmentStatus.PARTIALLY_DISPOSED;
                }

                BigDecimal remainingValue = money(investment.getCurrentValue()).subtract(proceedsAmount);
                yield remainingValue.compareTo(ZERO_MONEY) <= 0
                        ? InvestmentStatus.CLOSED
                        : InvestmentStatus.PARTIALLY_DISPOSED;
            }
        };
    }

    private void applyDisposal(Investment investment,
                               DisposeInvestmentRequest request,
                               InvestmentStatus targetStatus,
                               BigDecimal proceedsAmount,
                               BigDecimal fees) {
        if (request.disposalType() == DisposalType.PARTIAL) {
            applyPartialDisposal(investment, request, proceedsAmount);
        } else if (request.disposalType() == DisposalType.FULL) {
            investment.setCurrentValue(ZERO_MONEY);
            if (isUnitType(investment.getInvestmentType()) && investment.getUnits() != null) {
                investment.setUnits(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            }
        } else {
            investment.setCurrentValue(money(proceedsAmount.subtract(fees)));
            if (isUnitType(investment.getInvestmentType()) && investment.getUnits() != null) {
                investment.setUnits(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            }
            if (investment.getMaturityDate() == null) {
                investment.setMaturityDate(request.disposalDate());
            }
        }

        investment.setStatus(targetStatus);
    }

    private void applyPartialDisposal(Investment investment,
                                      DisposeInvestmentRequest request,
                                      BigDecimal proceedsAmount) {
        if (isUnitType(investment.getInvestmentType()) && investment.getUnits() != null) {
            if (request.unitsRedeemed() == null) {
                throw new BusinessException("Units redeemed are required for partial disposal of unit investments");
            }

            BigDecimal redeemed = units(request.unitsRedeemed());
            BigDecimal currentUnits = units(investment.getUnits());
            if (redeemed.compareTo(currentUnits) > 0) {
                throw new BusinessException("Units redeemed cannot exceed units held");
            }

            BigDecimal remainingUnits = units(currentUnits.subtract(redeemed));
            investment.setUnits(remainingUnits);

            if (investment.getNavPerUnit() != null) {
                investment.setCurrentValue(money(remainingUnits.multiply(investment.getNavPerUnit())));
            } else {
                BigDecimal ratio = remainingUnits
                        .divide(currentUnits, 8, RoundingMode.HALF_UP);
                investment.setCurrentValue(money(investment.getCurrentValue().multiply(ratio)));
            }
            return;
        }

        BigDecimal remainingValue = money(investment.getCurrentValue()).subtract(proceedsAmount);
        investment.setCurrentValue(remainingValue.max(ZERO_MONEY));
    }

    private String generateUniqueReferenceNumber() {
        for (int attempt = 0; attempt < MAX_REFERENCE_GENERATION_ATTEMPTS; attempt++) {
            String candidate = SecureIdGenerator.generate("INV");
            if (!investmentRepository.existsByReferenceNumber(candidate)) {
                return candidate;
            }
        }

        throw new BusinessException(
                "Unable to generate unique investment reference after " + MAX_REFERENCE_GENERATION_ATTEMPTS + " attempts"
        );
    }

    private boolean isUnitType(InvestmentType investmentType) {
        return UNIT_TYPES.contains(investmentType);
    }

    private BigDecimal sumMoney(List<BigDecimal> values) {
        return money(values.stream()
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        return amount.multiply(HUNDRED).divide(total, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) {
            return ZERO_MONEY;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nav(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal units(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private String trimmedOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String mergeNotes(String currentNotes, String newNote) {
        String trimmedCurrent = trimmedOrNull(currentNotes);
        String trimmedNew = trimmedOrNull(newNote);

        if (trimmedNew == null) {
            return trimmedCurrent;
        }
        if (trimmedCurrent == null) {
            return trimmedNew;
        }
        return trimmedCurrent + "\n" + trimmedNew;
    }
}
