package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.common.event.PenaltyAppliedEvent;
import com.innercircle.sacco.loan.entity.LoanPenalty;
import com.innercircle.sacco.loan.repository.LoanPenaltyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

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
        eventPublisher.publishEvent(new PenaltyAppliedEvent(
                savedPenalty.getId(),
                memberId,
                amount,
                "LOAN_LATE_REPAYMENT",
                actor
        ));

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
}
