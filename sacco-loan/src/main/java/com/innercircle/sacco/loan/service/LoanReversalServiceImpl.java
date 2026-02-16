package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.common.event.LoanReversalEvent;
import com.innercircle.sacco.loan.dto.ReversalResponse;
import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanRepayment;
import com.innercircle.sacco.loan.entity.LoanStatus;
import com.innercircle.sacco.loan.entity.RepaymentSchedule;
import com.innercircle.sacco.loan.entity.RepaymentStatus;
import com.innercircle.sacco.loan.repository.LoanApplicationRepository;
import com.innercircle.sacco.loan.repository.LoanRepaymentRepository;
import com.innercircle.sacco.loan.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanReversalServiceImpl implements LoanReversalService {

    private final LoanRepaymentRepository repaymentRepository;
    private final LoanApplicationRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ReversalResponse reverseRepayment(UUID repaymentId, String reason, String actor) {
        log.info("Reversing repayment {} by {}, reason: {}", repaymentId, actor, reason);

        // Get the repayment
        LoanRepayment repayment = repaymentRepository.findById(repaymentId)
                .orElseThrow(() -> new IllegalArgumentException("Repayment not found with id: " + repaymentId));

        // Check if already reversed
        if (repayment.getStatus() == RepaymentStatus.REVERSED) {
            throw new IllegalStateException("Repayment is already reversed");
        }

        // Get the loan
        LoanApplication loan = loanRepository.findById(repayment.getLoanId())
                .orElseThrow(() -> new IllegalArgumentException("Loan not found with id: " + repayment.getLoanId()));

        // Mark repayment as REVERSED
        repayment.setStatus(RepaymentStatus.REVERSED);
        repaymentRepository.save(repayment);

        // Validate balance won't go negative
        BigDecimal newTotalRepaid = loan.getTotalRepaid().subtract(repayment.getAmount());
        if (newTotalRepaid.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Reversal would result in negative total repaid");
        }

        // Restore loan outstanding balance
        loan.setTotalRepaid(newTotalRepaid);
        loan.setOutstandingBalance(loan.getOutstandingBalance().add(repayment.getAmount()));

        // If loan was closed, reopen it
        if (loan.getStatus() == LoanStatus.CLOSED) {
            loan.setStatus(LoanStatus.REPAYING);
            log.info("Loan {} reopened from CLOSED to REPAYING due to reversal", loan.getId());
        }

        loanRepository.save(loan);

        // Un-mark repayment schedule installments
        // Reverse from the latest paid installment backward (LIFO order)
        List<RepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumber(loan.getId());
        schedules.sort(Comparator.comparing(RepaymentSchedule::getInstallmentNumber).reversed());

        BigDecimal remainingToReverse = repayment.getAmount();

        for (RepaymentSchedule schedule : schedules) {
            if (remainingToReverse.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            if (schedule.getAmountPaid() != null && schedule.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal amountToReverse = remainingToReverse.min(schedule.getAmountPaid());

                schedule.setAmountPaid(schedule.getAmountPaid().subtract(amountToReverse));
                if (schedule.getAmountPaid().compareTo(schedule.getTotalAmount()) < 0) {
                    schedule.setPaid(false);
                }

                scheduleRepository.save(schedule);
                remainingToReverse = remainingToReverse.subtract(amountToReverse);

                log.info("Reversed {} from schedule installment #{} for loan {}",
                    amountToReverse, schedule.getInstallmentNumber(), loan.getId());
            }
        }

        Instant reversedAt = Instant.now();

        // Publish reversal event for ledger compensation
        eventPublisher.publishEvent(new LoanReversalEvent(
                UUID.randomUUID(), // Generate a reversal ID
                "REPAYMENT",
                repaymentId,
                loan.getId(),
                loan.getMemberId(),
                repayment.getAmount(),
                repayment.getPrincipalPortion(),
                repayment.getInterestPortion(),
                repayment.getPenaltyPortion(),
                reason,
                UUID.randomUUID(),
                actor
        ));

        log.info("Repayment {} reversed successfully", repaymentId);

        return ReversalResponse.builder()
                .reversalId(repaymentId)
                .reversalType("REPAYMENT")
                .originalTransactionId(repaymentId)
                .loanId(loan.getId())
                .memberId(loan.getMemberId())
                .amount(repayment.getAmount())
                .reason(reason)
                .actor(actor)
                .reversedAt(reversedAt)
                .message("Repayment reversed successfully")
                .build();
    }

    @Override
    @Transactional
    public ReversalResponse reversePenalty(UUID penaltyId, String reason, String actor) {
        log.info("Reversing penalty {} by {}, reason: {}", penaltyId, actor, reason);

        // Note: Since we don't have a Penalty entity yet, this is a placeholder implementation
        // When penalties are implemented, this would:
        // 1. Mark penalty as reversed
        // 2. Restore loan balances if penalty was added to outstanding
        // 3. Publish event for ledger compensation

        throw new UnsupportedOperationException(
            "Penalty reversal not yet implemented - penalty system pending");
    }
}
