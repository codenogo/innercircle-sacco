package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.common.event.PenaltyAppliedEvent;
import com.innercircle.sacco.common.event.PenaltyPaidEvent;
import com.innercircle.sacco.common.outbox.EventOutboxWriter;
import com.innercircle.sacco.loan.entity.LoanPenalty;
import com.innercircle.sacco.loan.repository.LoanPenaltyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanPenaltyServiceImpl implements LoanPenaltyService {

    private final LoanPenaltyRepository penaltyRepository;
    private final EventOutboxWriter outboxWriter;

    @Override
    @Transactional
    public LoanPenalty applyPenalty(UUID loanId, UUID memberId, BigDecimal amount, String reason, String actor) {
        return applyPenalty(loanId, memberId, amount, reason, actor, null);
    }

    @Override
    @Transactional
    public LoanPenalty applyPenalty(UUID loanId, UUID memberId, BigDecimal amount, String reason, String actor, UUID scheduleId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Penalty amount must be greater than zero");
        }

        // Idempotency: if scheduleId provided, check if penalty already exists for this schedule
        if (scheduleId != null) {
            List<LoanPenalty> existing = penaltyRepository.findByLoanIdAndScheduleId(loanId, scheduleId);
            if (!existing.isEmpty()) {
                log.info("Penalty already applied for loan {} schedule {}, skipping", loanId, scheduleId);
                return existing.get(0);
            }
        }

        LoanPenalty penalty = new LoanPenalty();
        penalty.setLoanId(loanId);
        penalty.setMemberId(memberId);
        penalty.setAmount(amount);
        penalty.setReason(reason);
        penalty.setApplied(true);
        penalty.setAppliedAt(Instant.now());
        penalty.setScheduleId(scheduleId);

        LoanPenalty savedPenalty = penaltyRepository.save(penalty);

        // Publish penalty applied event
        outboxWriter.write(new PenaltyAppliedEvent(
                savedPenalty.getId(),
                memberId,
                amount,
                "LOAN_LATE_REPAYMENT",
                UUID.randomUUID(),
                actor
        ), "LoanApplication", savedPenalty.getId());

        log.info("Applied penalty {} of {} for loan {} (schedule: {})",
                savedPenalty.getId(), amount, loanId, scheduleId);

        return savedPenalty;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanPenalty> getLoanPenalties(UUID loanId) {
        return penaltyRepository.findByLoanId(loanId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanPenalty> getMemberPenalties(UUID memberId) {
        return penaltyRepository.findByMemberId(memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanPenalty> getUnpaidPenalties(UUID loanId) {
        return penaltyRepository.findByLoanIdAndPaidFalse(loanId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalUnpaidPenalties(UUID loanId) {
        return penaltyRepository.sumUnpaidAmountByLoanId(loanId);
    }

    @Override
    @Transactional
    public void markPenaltyPaid(UUID penaltyId, String actor) {
        LoanPenalty penalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new IllegalArgumentException("Penalty not found: " + penaltyId));

        if (Boolean.TRUE.equals(penalty.getPaid())) {
            log.info("Penalty {} already paid, skipping", penaltyId);
            return;
        }

        penalty.setPaid(true);
        penalty.setPaidAt(Instant.now());
        penaltyRepository.save(penalty);

        outboxWriter.write(new PenaltyPaidEvent(
                penaltyId,
                penalty.getMemberId(),
                penalty.getAmount(),
                UUID.randomUUID(),
                actor
        ), "LoanApplication", penaltyId);

        log.info("Marked penalty {} as paid (amount: {})", penaltyId, penalty.getAmount());
    }

    @Override
    @Transactional
    public BigDecimal payPenalties(UUID loanId, BigDecimal availableAmount, String actor) {
        List<LoanPenalty> unpaidPenalties = penaltyRepository.findByLoanIdAndPaidFalseOrderByAppliedAtAsc(loanId);
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal remaining = availableAmount;
        int paidCount = 0;

        for (LoanPenalty penalty : unpaidPenalties) {
            if (remaining.compareTo(penalty.getAmount()) >= 0) {
                markPenaltyPaid(penalty.getId(), actor);
                remaining = remaining.subtract(penalty.getAmount());
                totalPaid = totalPaid.add(penalty.getAmount());
                paidCount++;
            }
            // Penalties are atomic — no partial payment; skip if insufficient
        }

        if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            log.info("Paid {} in penalties for loan {} ({} penalties)", totalPaid, loanId, paidCount);
        }

        return totalPaid;
    }
}
