package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.common.event.PenaltyAppliedEvent;
import com.innercircle.sacco.loan.entity.LoanPenalty;
import com.innercircle.sacco.loan.repository.LoanPenaltyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanPenaltyServiceImpl implements LoanPenaltyService {

    private final LoanPenaltyRepository penaltyRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public LoanPenalty applyPenalty(UUID loanId, UUID memberId, BigDecimal amount, String reason, String actor) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Penalty amount must be greater than zero");
        }

        LoanPenalty penalty = new LoanPenalty();
        penalty.setLoanId(loanId);
        penalty.setMemberId(memberId);
        penalty.setAmount(amount);
        penalty.setReason(reason);
        penalty.setApplied(true);
        penalty.setAppliedAt(Instant.now());

        LoanPenalty savedPenalty = penaltyRepository.save(penalty);

        // Publish penalty applied event
        eventPublisher.publishEvent(new PenaltyAppliedEvent(
                savedPenalty.getId(),
                memberId,
                amount,
                "LOAN_LATE_REPAYMENT",
                actor
        ));

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
}
