package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.loan.entity.LoanPenalty;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LoanPenaltyService {

    LoanPenalty applyPenalty(UUID loanId, UUID memberId, BigDecimal amount, String reason, String actor);

    LoanPenalty applyPenalty(UUID loanId, UUID memberId, BigDecimal amount, String reason, String actor, UUID scheduleId);

    List<LoanPenalty> getLoanPenalties(UUID loanId);

    List<LoanPenalty> getMemberPenalties(UUID memberId);

    List<LoanPenalty> getUnpaidPenalties(UUID loanId);

    BigDecimal getTotalUnpaidPenalties(UUID loanId);
}
