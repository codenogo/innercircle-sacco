package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.common.event.LoanApplicationEvent;
import com.innercircle.sacco.common.event.LoanDisbursedEvent;
import com.innercircle.sacco.common.event.LoanRepaymentEvent;
import com.innercircle.sacco.common.event.LoanStatusChangeEvent;
import com.innercircle.sacco.common.guard.MakerCheckerGuard;
import com.innercircle.sacco.common.outbox.EventOutboxWriter;
import com.innercircle.sacco.common.util.SecureIdGenerator;
import com.innercircle.sacco.config.entity.InterestMethod;
import com.innercircle.sacco.config.entity.LoanProductConfig;
import com.innercircle.sacco.config.service.ConfigService;
import com.innercircle.sacco.loan.entity.InterestEventType;
import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanInterestHistory;
import com.innercircle.sacco.loan.entity.LoanRepayment;
import com.innercircle.sacco.loan.entity.LoanStatus;
import com.innercircle.sacco.loan.entity.RepaymentSchedule;
import com.innercircle.sacco.loan.entity.RepaymentStatus;
import com.innercircle.sacco.loan.guard.LoanTransitionGuards;
import com.innercircle.sacco.loan.repository.LoanApplicationRepository;
import com.innercircle.sacco.loan.repository.LoanInterestHistoryRepository;
import com.innercircle.sacco.loan.repository.LoanRepaymentRepository;
import com.innercircle.sacco.loan.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private static final int MAX_GENERATION_ATTEMPTS = 5;
    private static final List<LoanStatus> POOL_CAP_ACTIVE_STATUSES = List.of(
            LoanStatus.APPROVED,
            LoanStatus.DISBURSED,
            LoanStatus.REPAYING,
            LoanStatus.DEFAULTED
    );

    private final LoanApplicationRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final LoanInterestHistoryRepository interestHistoryRepository;
    private final InterestCalculator interestCalculator;
    private final RepaymentScheduleGenerator scheduleGenerator;
    private final EventOutboxWriter outboxWriter;
    private final ConfigService configService;
    private final LoanPenaltyService loanPenaltyService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public LoanApplication applyForLoan(UUID memberId, UUID loanProductId, BigDecimal principalAmount,
                                         Integer termMonths, String purpose, String actor) {
        if (principalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Principal amount must be greater than zero");
        }

        if (termMonths <= 0) {
            throw new IllegalArgumentException("Term months must be greater than zero");
        }

        // Lookup loan product configuration
        LoanProductConfig product = configService.getLoanProduct(loanProductId);

        if (!product.isActive()) {
            throw new IllegalArgumentException("Loan product is not active");
        }

        // Enforce config limits
        if (principalAmount.compareTo(product.getMaxAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Principal amount exceeds maximum allowed: " + product.getMaxAmount());
        }

        if (principalAmount.compareTo(product.getMinAmount()) < 0) {
            throw new IllegalArgumentException(
                    "Principal amount is below minimum allowed: " + product.getMinAmount());
        }

        if (termMonths > product.getMaxTermMonths()) {
            throw new IllegalArgumentException(
                    "Term exceeds maximum allowed: " + product.getMaxTermMonths() + " months");
        }

        if (termMonths < product.getMinTermMonths()) {
            throw new IllegalArgumentException(
                    "Term is below minimum allowed: " + product.getMinTermMonths() + " months");
        }

        enforceContributionCap(memberId, principalAmount, product);
        enforcePoolCap(loanProductId, principalAmount, product);

        LoanApplication loan = new LoanApplication();
        loan.setMemberId(memberId);
        loan.setLoanProductId(loanProductId);
        loan.setPrincipalAmount(principalAmount);
        loan.setInterestRate(product.getAnnualInterestRate());
        loan.setTermMonths(termMonths);
        loan.setInterestMethod(product.getInterestMethod());
        loan.setPurpose(purpose);
        loan.setStatus(LoanStatus.PENDING);
        loan.setTotalRepaid(BigDecimal.ZERO);
        loan.setOutstandingBalance(BigDecimal.ZERO);
        loan.setTotalInterestAccrued(BigDecimal.ZERO);
        loan.setTotalInterestPaid(BigDecimal.ZERO);
        loan.setInterestAccrualEnabled(product.isInterestAccrualEnabled());
        loan.setCreatedBy(actor);
        loan.setLoanNumber(generateUniqueLoanNumber());

        LoanApplication savedLoan = loanRepository.save(loan);

        outboxWriter.write(new LoanApplicationEvent(
                savedLoan.getId(),
                savedLoan.getMemberId(),
                "APPLIED",
                UUID.randomUUID(),
                actor
        ), "LoanApplication", savedLoan.getId());

        return savedLoan;
    }

    @Override
    @Transactional
    public LoanApplication approveLoan(UUID loanId, UUID approvedBy, String actor,
                                        String overrideReason, boolean isAdmin) {
        LoanApplication loan = getLoanById(loanId);

        boolean overrideUsed = MakerCheckerGuard.assertOrOverride(
                loan.getCreatedBy(), actor, overrideReason, isAdmin,
                "LoanApplication", loan.getId());

        LoanTransitionGuards.LOAN.validate(loan.getStatus(), LoanStatus.APPROVED);

        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovedBy(approvedBy);
        loan.setApprovedAt(Instant.now());

        LoanApplication approved = loanRepository.save(loan);

        String action = overrideUsed ? "OVERRIDE_APPROVED" : "APPROVED";
        outboxWriter.write(new LoanApplicationEvent(
                approved.getId(),
                approved.getMemberId(),
                action,
                UUID.randomUUID(),
                actor
        ), "LoanApplication", approved.getId());

        return approved;
    }

    @Override
    @Transactional
    public LoanApplication rejectLoan(UUID loanId, UUID rejectedBy, String actor,
                                       String overrideReason, boolean isAdmin) {
        LoanApplication loan = getLoanById(loanId);

        boolean overrideUsed = MakerCheckerGuard.assertOrOverride(
                loan.getCreatedBy(), actor, overrideReason, isAdmin,
                "LoanApplication", loan.getId());

        LoanTransitionGuards.LOAN.validate(loan.getStatus(), LoanStatus.REJECTED);

        loan.setStatus(LoanStatus.REJECTED);
        loan.setApprovedBy(rejectedBy);
        loan.setApprovedAt(Instant.now());

        LoanApplication rejected = loanRepository.save(loan);

        String action = overrideUsed ? "OVERRIDE_REJECTED" : "REJECTED";
        outboxWriter.write(new LoanApplicationEvent(
                rejected.getId(),
                rejected.getMemberId(),
                action,
                UUID.randomUUID(),
                actor
        ), "LoanApplication", rejected.getId());

        return rejected;
    }

    @Override
    @Transactional
    public LoanApplication disburseLoan(UUID loanId, String actor) {
        LoanApplication loan = getLoanById(loanId);

        LoanTransitionGuards.LOAN.validate(loan.getStatus(), LoanStatus.DISBURSED);

        Instant now = Instant.now();
        LocalDate disbursementDate = LocalDate.ofInstant(now, ZoneId.systemDefault());

        loan.setStatus(LoanStatus.REPAYING);
        loan.setDisbursedAt(now);

        // Calculate total amount including interest
        BigDecimal totalInterest;
        if (loan.getInterestMethod() == InterestMethod.REDUCING_BALANCE) {
            totalInterest = interestCalculator.calculateReducingBalance(
                    loan.getPrincipalAmount(), loan.getInterestRate(), loan.getTermMonths());
        } else {
            totalInterest = interestCalculator.calculateFlatRate(
                    loan.getPrincipalAmount(), loan.getInterestRate(), loan.getTermMonths());
        }

        BigDecimal totalAmount = loan.getPrincipalAmount().add(totalInterest);
        loan.setOutstandingBalance(totalAmount);

        // Generate repayment schedule
        List<RepaymentSchedule> schedules = scheduleGenerator.generateSchedule(
                loanId,
                loan.getPrincipalAmount(),
                loan.getInterestRate(),
                loan.getTermMonths(),
                loan.getInterestMethod(),
                disbursementDate
        );

        scheduleRepository.saveAll(schedules);

        LoanApplication savedLoan = loanRepository.save(loan);

        // Publish disbursement event
        outboxWriter.write(new LoanDisbursedEvent(
                loanId,
                loan.getMemberId(),
                loan.getPrincipalAmount(),
                totalInterest,
                UUID.randomUUID(),
                actor
        ), "LoanApplication", loanId);

        return savedLoan;
    }

    @Override
    @Transactional
    public LoanRepayment recordRepayment(UUID loanId, BigDecimal amount, String referenceNumber, String actor) {
        LoanApplication loan = getLoanById(loanId);

        if (loan.getStatus() != LoanStatus.REPAYING && loan.getStatus() != LoanStatus.DISBURSED) {
            throw new IllegalStateException("Loan is not in a repayable state");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Repayment amount must be greater than zero");
        }

        if (amount.compareTo(loan.getOutstandingBalance()) > 0) {
            throw new IllegalArgumentException("Repayment amount exceeds outstanding balance");
        }

        // Interest-first allocation: overdue interest → current interest → principal
        BigDecimal unpaidInterest = loan.getTotalInterestAccrued()
                .subtract(loan.getTotalInterestPaid())
                .max(BigDecimal.ZERO);

        BigDecimal remainingAmount = amount;
        BigDecimal totalInterestPaid = BigDecimal.ZERO;
        BigDecimal totalPrincipalPaid = BigDecimal.ZERO;

        // Step 1: Allocate to accrued but unpaid interest first
        if (unpaidInterest.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal interestPayment = remainingAmount.min(unpaidInterest);
            totalInterestPaid = totalInterestPaid.add(interestPayment);
            remainingAmount = remainingAmount.subtract(interestPayment);
        }

        // Step 2: Allocate remaining to unpaid penalties (oldest first, atomic — no partial)
        BigDecimal totalPenaltyPaid = BigDecimal.ZERO;
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            totalPenaltyPaid = loanPenaltyService.payPenalties(loanId, remainingAmount, actor);
            remainingAmount = remainingAmount.subtract(totalPenaltyPaid);
        }

        // Step 3: Allocate remaining to principal via schedule installments
        List<RepaymentSchedule> unpaidSchedules = scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId);
        for (RepaymentSchedule schedule : unpaidSchedules) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal scheduleOutstanding = schedule.getTotalAmount().subtract(schedule.getAmountPaid());
            if (scheduleOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal paymentForSchedule = remainingAmount.min(scheduleOutstanding);

            // With interest-first, schedule payments go toward principal
            totalPrincipalPaid = totalPrincipalPaid.add(paymentForSchedule);

            schedule.setAmountPaid(schedule.getAmountPaid().add(paymentForSchedule));
            if (schedule.getAmountPaid().compareTo(schedule.getTotalAmount()) >= 0) {
                schedule.setPaid(true);
            }
            scheduleRepository.save(schedule);

            remainingAmount = remainingAmount.subtract(paymentForSchedule);
        }

        // Update totalInterestPaid on the loan
        loan.setTotalInterestPaid(loan.getTotalInterestPaid().add(totalInterestPaid));

        // Create interest history record for repayment if interest was paid
        if (totalInterestPaid.compareTo(BigDecimal.ZERO) > 0) {
            LoanInterestHistory history = new LoanInterestHistory();
            history.setLoanId(loanId);
            history.setMemberId(loan.getMemberId());
            history.setAccrualDate(LocalDate.now());
            history.setInterestAmount(totalInterestPaid.negate());
            history.setOutstandingBalanceSnapshot(loan.getOutstandingBalance());
            history.setCumulativeInterestAccrued(loan.getTotalInterestAccrued());
            history.setInterestRate(loan.getInterestRate());
            history.setEventType(InterestEventType.REPAYMENT_APPLIED);
            history.setDescription("Interest paid via repayment " + referenceNumber);
            interestHistoryRepository.save(history);
        }

        // Record the repayment
        LoanRepayment repayment = new LoanRepayment();
        repayment.setLoanId(loanId);
        repayment.setMemberId(loan.getMemberId());
        repayment.setAmount(amount);
        repayment.setPrincipalPortion(totalPrincipalPaid);
        repayment.setInterestPortion(totalInterestPaid);
        repayment.setPenaltyPortion(totalPenaltyPaid);
        repayment.setRepaymentDate(LocalDate.now());
        repayment.setReferenceNumber(referenceNumber);
        repayment.setStatus(RepaymentStatus.CONFIRMED);

        LoanRepayment savedRepayment = repaymentRepository.save(repayment);

        // Update loan totals
        loan.setTotalRepaid(loan.getTotalRepaid().add(amount));
        loan.setOutstandingBalance(loan.getOutstandingBalance().subtract(amount));

        // Check if loan is fully paid
        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(LoanStatus.CLOSED);
        }

        loanRepository.save(loan);

        // Publish repayment event
        outboxWriter.write(new LoanRepaymentEvent(
                loanId,
                loan.getMemberId(),
                savedRepayment.getId(),
                amount,
                totalPrincipalPaid,
                totalInterestPaid,
                totalPenaltyPaid,
                UUID.randomUUID(),
                actor
        ), "LoanApplication", loanId);

        return savedRepayment;
    }

    @Override
    @Transactional
    public LoanApplication closeLoan(UUID loanId) {
        LoanApplication loan = getLoanById(loanId);

        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Cannot close loan with outstanding balance");
        }

        String previousStatus = loan.getStatus().name();
        loan.setStatus(LoanStatus.CLOSED);
        LoanApplication closed = loanRepository.save(loan);

        outboxWriter.write(new LoanStatusChangeEvent(
                closed.getId(),
                previousStatus,
                "CLOSED",
                UUID.randomUUID(),
                "system"
        ), "LoanApplication", closed.getId());

        return closed;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepaymentSchedule> getLoanSchedule(UUID loanId) {
        return scheduleRepository.findByLoanIdOrderByInstallmentNumber(loanId);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanApplication getLoanById(UUID loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found with id: " + loanId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanApplication> getMemberLoans(UUID memberId) {
        return loanRepository.findByMemberId(memberId);
    }

    private void enforceContributionCap(UUID memberId, BigDecimal principalAmount, LoanProductConfig product) {
        if (product.getContributionCapPercent() == null) {
            return;
        }

        BigDecimal confirmedContributions = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(contribution_amount), 0) FROM contributions " +
                        "WHERE member_id = ? AND status = 'CONFIRMED'",
                BigDecimal.class,
                memberId
        );
        if (confirmedContributions == null) {
            confirmedContributions = BigDecimal.ZERO;
        }

        BigDecimal capAmount = confirmedContributions.multiply(product.getContributionCapPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (principalAmount.compareTo(capAmount) > 0) {
            throw new IllegalArgumentException(
                    "Requested amount exceeds contribution cap. Maximum allowed: " + capAmount);
        }
    }

    private void enforcePoolCap(UUID loanProductId, BigDecimal principalAmount, LoanProductConfig product) {
        if (product.getPoolCapAmount() == null || product.getPoolCapAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal activePool = loanRepository.sumOutstandingBalanceByLoanProductIdAndStatusIn(
                loanProductId,
                POOL_CAP_ACTIVE_STATUSES
        );
        if (activePool == null) {
            activePool = BigDecimal.ZERO;
        }

        BigDecimal projected = activePool.add(principalAmount);
        if (projected.compareTo(product.getPoolCapAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Loan pool cap exceeded for product. Available pool balance: " +
                            product.getPoolCapAmount().subtract(activePool).max(BigDecimal.ZERO));
        }
    }

    private String generateUniqueLoanNumber() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = SecureIdGenerator.generate("LN");
            if (!loanRepository.existsByLoanNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Unable to generate unique loan number after " + MAX_GENERATION_ATTEMPTS + " attempts");
    }
}
