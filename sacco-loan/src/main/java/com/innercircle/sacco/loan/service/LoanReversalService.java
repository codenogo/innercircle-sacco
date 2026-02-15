package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.loan.dto.ReversalResponse;

import java.util.UUID;

public interface LoanReversalService {

    /**
     * Reverse a loan repayment - marks repayment as REVERSED,
     * restores loan outstanding balance, un-marks schedule installments.
     *
     * @param repaymentId the repayment ID to reverse
     * @param reason the reason for reversal
     * @param actor the user performing the reversal
     * @return ReversalResponse with reversal details
     */
    ReversalResponse reverseRepayment(UUID repaymentId, String reason, String actor);

    /**
     * Reverse a penalty application.
     *
     * @param penaltyId the penalty ID to reverse
     * @param reason the reason for reversal
     * @param actor the user performing the reversal
     * @return ReversalResponse with reversal details
     */
    ReversalResponse reversePenalty(UUID penaltyId, String reason, String actor);
}
