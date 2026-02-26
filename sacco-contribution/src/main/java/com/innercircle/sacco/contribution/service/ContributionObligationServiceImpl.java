package com.innercircle.sacco.contribution.service;

import com.innercircle.sacco.config.entity.ContributionScheduleConfig;
import com.innercircle.sacco.config.entity.PenaltyRule;
import com.innercircle.sacco.config.entity.PenaltyRuleTier;
import com.innercircle.sacco.config.repository.ContributionScheduleConfigRepository;
import com.innercircle.sacco.config.service.ConfigService;
import com.innercircle.sacco.contribution.entity.Contribution;
import com.innercircle.sacco.contribution.entity.ContributionObligation;
import com.innercircle.sacco.contribution.entity.ContributionObligationStatus;
import com.innercircle.sacco.contribution.entity.ContributionPenalty;
import com.innercircle.sacco.contribution.repository.ContributionObligationRepository;
import com.innercircle.sacco.contribution.repository.ContributionPenaltyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContributionObligationServiceImpl implements ContributionObligationService {

    private final ContributionObligationRepository obligationRepository;
    private final ContributionPenaltyRepository penaltyRepository;
    private final ContributionPenaltyService penaltyService;
    private final ContributionScheduleConfigRepository scheduleConfigRepository;
    private final ConfigService configService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public List<ContributionObligation> runMonthlyObligations(YearMonth month, String actor) {
        LocalDate obligationMonth = month.atDay(1);
        List<UUID> activeMembers = jdbcTemplate.queryForList(
                "SELECT id FROM members WHERE status = 'ACTIVE'",
                UUID.class
        );
        List<ContributionScheduleConfig> schedules = scheduleConfigRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(ContributionScheduleConfig::isMandatory)
                .toList();

        List<ContributionObligation> created = new ArrayList<>();
        for (UUID memberId : activeMembers) {
            for (ContributionScheduleConfig schedule : schedules) {
                if (obligationRepository.existsByMemberIdAndScheduleConfigIdAndObligationMonth(
                        memberId,
                        schedule.getId(),
                        obligationMonth
                )) {
                    continue;
                }
                ContributionObligation obligation = new ContributionObligation();
                obligation.setMemberId(memberId);
                obligation.setScheduleConfigId(schedule.getId());
                obligation.setObligationMonth(obligationMonth);
                obligation.setDueDate(resolveDueDate(month, schedule.getDueDayOfMonth()));
                obligation.setGrossAmount(schedule.getExpectedGrossAmount());
                obligation.setPaidAmount(BigDecimal.ZERO);
                obligation.setPenaltyAmount(BigDecimal.ZERO);
                obligation.setStatus(ContributionObligationStatus.PENDING);
                obligation.setNotes("Generated from schedule: " + schedule.getName());
                obligation.setCreatedBy(actor);
                created.add(obligation);
            }
        }

        if (!created.isEmpty()) {
            obligationRepository.saveAll(created);
            log.info("Generated {} contribution obligations for {}", created.size(), month);
        }

        generateLatePenalties(month, actor);

        return getObligations(month, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContributionObligation> getObligations(YearMonth month, UUID memberId) {
        LocalDate obligationMonth = month.atDay(1);
        List<ContributionObligation> obligations = memberId == null
                ? obligationRepository.findByObligationMonthOrderByDueDateAsc(obligationMonth)
                : obligationRepository.findByMemberIdAndObligationMonthOrderByDueDateAsc(memberId, obligationMonth);

        LocalDate today = LocalDate.now();
        for (ContributionObligation obligation : obligations) {
            refreshStatus(obligation, today);
        }
        return obligations;
    }

    @Override
    @Transactional(readOnly = true)
    public UUID resolveObligationIdForContribution(UUID memberId, LocalDate contributionMonth) {
        LocalDate month = contributionMonth.withDayOfMonth(1);
        List<ContributionObligation> monthObligations = obligationRepository
                .findByMemberIdAndObligationMonthOrderByDueDateAsc(memberId, month);

        LocalDate today = LocalDate.now();
        for (ContributionObligation obligation : monthObligations) {
            refreshStatus(obligation, today);
            if (obligation.getStatus() != ContributionObligationStatus.PAID
                    && obligation.getStatus() != ContributionObligationStatus.WAIVED) {
                return obligation.getId();
            }
        }

        return null;
    }

    @Override
    @Transactional
    public void applyConfirmedContribution(Contribution contribution) {
        if (contribution.getObligationId() == null) {
            return;
        }

        Optional<ContributionObligation> obligationOpt = obligationRepository.findById(contribution.getObligationId());
        if (obligationOpt.isEmpty()) {
            return;
        }

        ContributionObligation obligation = obligationOpt.get();
        obligation.setPaidAmount(obligation.getPaidAmount().add(contribution.getAmount()));
        refreshStatus(obligation, LocalDate.now());
        obligationRepository.save(obligation);
    }

    @Override
    @Transactional
    public void reverseConfirmedContribution(Contribution contribution) {
        if (contribution.getObligationId() == null) {
            return;
        }

        Optional<ContributionObligation> obligationOpt = obligationRepository.findById(contribution.getObligationId());
        if (obligationOpt.isEmpty()) {
            return;
        }

        ContributionObligation obligation = obligationOpt.get();
        BigDecimal nextPaid = obligation.getPaidAmount().subtract(contribution.getAmount());
        obligation.setPaidAmount(nextPaid.max(BigDecimal.ZERO));
        refreshStatus(obligation, LocalDate.now());
        obligationRepository.save(obligation);
    }

    private LocalDate resolveDueDate(YearMonth month, Integer dueDayOfMonth) {
        int day = Math.min(Math.max(dueDayOfMonth, 1), month.lengthOfMonth());
        return month.atDay(day);
    }

    private void refreshStatus(ContributionObligation obligation, LocalDate today) {
        if (obligation.getStatus() == ContributionObligationStatus.WAIVED) {
            return;
        }

        if (obligation.getPaidAmount().compareTo(obligation.getGrossAmount()) >= 0) {
            obligation.setStatus(ContributionObligationStatus.PAID);
            return;
        }

        if (today.isAfter(obligation.getDueDate())) {
            obligation.setStatus(ContributionObligationStatus.OVERDUE);
            return;
        }

        if (obligation.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            obligation.setStatus(ContributionObligationStatus.PARTIALLY_PAID);
        } else {
            obligation.setStatus(ContributionObligationStatus.PENDING);
        }
    }

    private void generateLatePenalties(YearMonth month, String actor) {
        Optional<PenaltyRule> penaltyRuleOpt = configService.getActivePenaltyRuleByType(
                PenaltyRule.PenaltyType.LATE_CONTRIBUTION
        );
        if (penaltyRuleOpt.isEmpty()) {
            return;
        }

        PenaltyRule rule = penaltyRuleOpt.get();
        List<PenaltyRuleTier> tiers = rule.getTiers().stream()
                .filter(PenaltyRuleTier::isActive)
                .sorted(Comparator.comparing(PenaltyRuleTier::getSequence))
                .toList();
        if (tiers.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        List<ContributionObligation> obligations = obligationRepository.findByObligationMonthOrderByDueDateAsc(month.atDay(1));
        for (ContributionObligation obligation : obligations) {
            refreshStatus(obligation, today);
            if (obligation.getStatus() == ContributionObligationStatus.PAID
                    || obligation.getStatus() == ContributionObligationStatus.WAIVED) {
                continue;
            }
            if (!today.isAfter(obligation.getDueDate())) {
                continue;
            }

            long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(obligation.getDueDate(), today);
            BigDecimal overdueAmount = obligation.getGrossAmount().subtract(obligation.getPaidAmount()).max(BigDecimal.ZERO);
            if (overdueAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            for (PenaltyRuleTier tier : tiers) {
                if (!matchesTier(tier, overdueDays)) {
                    continue;
                }
                applyTierPenaltyForMonth(obligation, tier, overdueAmount, monthStart, monthEnd, today, actor);
            }

            obligationRepository.save(obligation);
        }
    }

    private boolean matchesTier(PenaltyRuleTier tier, long overdueDays) {
        if (overdueDays < tier.getStartOverdueDay()) {
            return false;
        }
        return tier.getEndOverdueDay() == null || overdueDays <= tier.getEndOverdueDay();
    }

    private void applyTierPenaltyForMonth(
            ContributionObligation obligation,
            PenaltyRuleTier tier,
            BigDecimal overdueAmount,
            LocalDate monthStart,
            LocalDate monthEnd,
            LocalDate today,
            String actor
    ) {
        LocalDate tierStartDate = obligation.getDueDate().plusDays(tier.getStartOverdueDay());
        LocalDate tierEndDate = tier.getEndOverdueDay() == null
                ? today
                : obligation.getDueDate().plusDays(tier.getEndOverdueDay());

        LocalDate from = maxDate(monthStart, tierStartDate);
        LocalDate to = minDate(minDate(monthEnd, tierEndDate), today);
        if (to.isBefore(from)) {
            return;
        }

        switch (tier.getFrequency()) {
            case DAILY -> {
                LocalDate cursor = from;
                while (!cursor.isAfter(to)) {
                    applyPenaltyIfMissing(obligation, tier, overdueAmount, cursor, actor);
                    cursor = cursor.plusDays(1);
                }
            }
            case MONTHLY -> applyPenaltyIfMissing(obligation, tier, overdueAmount, from.withDayOfMonth(1), actor);
            case ONCE -> applyPenaltyIfMissing(obligation, tier, overdueAmount, tierStartDate, actor);
        }
    }

    private void applyPenaltyIfMissing(
            ContributionObligation obligation,
            PenaltyRuleTier tier,
            BigDecimal overdueAmount,
            LocalDate penaltyDate,
            String actor
    ) {
        String penaltyCode = String.format(
                "OBL-%s-T%s-D%s",
                obligation.getId(),
                tier.getId(),
                penaltyDate
        );
        if (penaltyRepository.existsByPenaltyCode(penaltyCode)) {
            return;
        }

        BigDecimal amount = calculatePenaltyAmount(overdueAmount, tier);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        ContributionPenalty penalty = new ContributionPenalty();
        penalty.setMemberId(obligation.getMemberId());
        penalty.setContributionId(null);
        penalty.setObligationId(obligation.getId());
        penalty.setRuleTierId(tier.getId());
        penalty.setAmount(amount);
        penalty.setReason("Late contribution obligation penalty");
        penalty.setPenaltyCode(penaltyCode);
        penalty.setPenaltyDate(penaltyDate);

        penaltyService.applyPenalty(penalty, actor);
        obligation.setPenaltyAmount(obligation.getPenaltyAmount().add(amount));
    }

    private BigDecimal calculatePenaltyAmount(BigDecimal overdueAmount, PenaltyRuleTier tier) {
        if (tier.getCalculationMethod() == PenaltyRule.CalculationMethod.FLAT) {
            return tier.getRate().setScale(2, RoundingMode.HALF_UP);
        }
        return overdueAmount.multiply(tier.getRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private LocalDate minDate(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    private LocalDate maxDate(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }
}
