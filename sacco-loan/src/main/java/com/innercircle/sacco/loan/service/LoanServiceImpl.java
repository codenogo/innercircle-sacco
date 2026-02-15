package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.common.event.LoanDisbursedEvent;
import com.innercircle.sacco.common.event.LoanRepaymentEvent;
import com.innercircle.sacco.config.entity.InterestMethod;
import com.innercircle.sacco.config.entity.LoanProductConfig;
import com.innercircle.sacco.config.service.ConfigService;
import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanRepayment;
import com.innercircle.sacco.loan.entity.LoanStatus;
import com.innercircle.sacco.loan.entity.RepaymentSchedule;
import com.innercircle.sacco.loan.entity.RepaymentStatus;
import com.innercircle.sacco.loan.repository.LoanApplicationRepository;
import com.innercircle.sacco.loan.repository.LoanRepaymentRepository;
import com.innercircle.sacco.loan.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanApplicationRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final InterestCalculator interestCalculator;
    private final RepaymentScheduleGenerator scheduleGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final ConfigService configService;

    @Override
    @Transactional
    public LoanApplication applyForLoan(UUID memberId, UUID loanProductId, BigDecimal principalAmount,
                                         Integer termMonths, String purpose) {
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

        if (termMonths > product.getMaxTermMonths()) {
            throw new IllegalArgumentException(
                    "Term exceeds maximum allowed: " + product.getMaxTermMonths() + " months");
        }

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

        return loanRepository.save(loan);
    }

    @Override
    @Transactional
    public LoanApplication approveLoan(UUID loanId, UUID approvedBy) {
        LoanApplication loan = getLoanById(loanId);

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException("Only pending loans can be approved");
        }

        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovedBy(approvedBy);
        loan.setApprovedAt(Instant.now());

        return loanRepository.save(loan);
    }

    @Override
    @Transactional
    public LoanApplication rejectLoan(UUID loanId, UUID rejectedBy) {
        LoanApplication loan = getLoanById(loanId);

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException("Only pending loans can be rejected");
        }

        loan.setStatus(LoanStatus.REJECTED);
        loan.setApprovedBy(rejectedBy);
        loan.setApprovedAt(Instant.now());

        return loanRepository.save(loan);
    }

    @Override
    @Transactional
    public LoanApplication disburseLoan(UUID loanId, String actor) {
        LoanApplication loan = getLoanById(loanId);

        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new IllegalStateException("Only approved loans can be disbursed");
        }

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
        eventPublisher.publishEvent(new LoanDisbursedEvent(
                loanId,
                loan.getMemberId(),
                loan.getPrincipalAmount(),
                totalInterest,
                actor
        ));

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

        // Get unpaid schedules in order
        List<RepaymentSchedule> unpaidSchedules = scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId);

        BigDecimal remainingAmount = amount;
        BigDecimal totalPrincipalPaid = BigDecimal.ZERO;
        BigDecimal totalInterestPaid = BigDecimal.ZERO;

        // Allocate payment to schedules
        for (RepaymentSchedule schedule : unpaidSchedules) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal scheduleOutstanding = schedule.getTotalAmount().subtract(schedule.getAmountPaid());
            if (scheduleOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal paymentForSchedule = remainingAmount.min(scheduleOutstanding);

            // Proportionally allocate to interest and principal
            BigDecimal interestPortion = schedule.getInterestAmount()
                    .multiply(paymentForSchedule)
                    .divide(schedule.getTotalAmount(), 2, java.math.RoundingMode.HALF_UP);

            BigDecimal principalPortion = paymentForSchedule.subtract(interestPortion);

            totalInterestPaid = totalInterestPaid.add(interestPortion);
            totalPrincipalPaid = totalPrincipalPaid.add(principalPortion);

            // Accumulate partial payment
            schedule.setAmountPaid(schedule.getAmountPaid().add(paymentForSchedule));
            if (schedule.getAmountPaid().compareTo(schedule.getTotalAmount()) >= 0) {
                schedule.setPaid(true);
            }
            scheduleRepository.save(schedule);

            remainingAmount = remainingAmount.subtract(paymentForSchedule);
        }

        // Record the repayment
        LoanRepayment repayment = new LoanRepayment();
        repayment.setLoanId(loanId);
        repayment.setMemberId(loan.getMemberId());
        repayment.setAmount(amount);
        repayment.setPrincipalPortion(totalPrincipalPaid);
        repayment.setInterestPortion(totalInterestPaid);
        repayment.setRepaymentDate(LocalDate.now());
        repayment.setReferenceNumber(referenceNumber);
        repayment.setStatus(RepaymentStatus.CONFIRMED);

        LoanRepayment savedRepayment = repaymentRepository.save(repayment);

        // Update loan totals
        loan.setTotalRepaid(loan.getTotalRepaid().add(amount));
        loan.setOutstandingBalance(loan.getOutstandingBalance().subtract(amount));

        // Check if loan is fully paid
        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.CLOSED);
        }

        loanRepository.save(loan);

        // Publish repayment event
        eventPublisher.publishEvent(new LoanRepaymentEvent(
                loanId,
                loan.getMemberId(),
                savedRepayment.getId(),
                amount,
                totalPrincipalPaid,
                totalInterestPaid,
                actor
        ));

        return savedRepayment;
    }

    @Override
    @Transactional
    public LoanApplication closeLoan(UUID loanId) {
        LoanApplication loan = getLoanById(loanId);

        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Cannot close loan with outstanding balance");
        }

        loan.setStatus(LoanStatus.CLOSED);
        return loanRepository.save(loan);
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
}
