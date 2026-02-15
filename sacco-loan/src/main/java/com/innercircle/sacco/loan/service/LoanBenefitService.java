package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.loan.dto.LoanBenefitResponse;
import com.innercircle.sacco.loan.dto.MemberEarningsResponse;
import com.innercircle.sacco.loan.entity.LoanBenefit;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LoanBenefitService {

    /**
     * Distributes interest earnings from a loan repayment to all active members
     * based on their share balance proportions.
     *
     * @param loanId the loan that generated the interest
     * @param interestAmount the total interest amount to distribute
     * @param actor the actor performing the distribution
     * @return list of created benefit records
     */
    List<LoanBenefit> distributeInterestEarnings(UUID loanId, BigDecimal interestAmount, String actor);

    /**
     * Gets aggregated earnings for a specific member.
     *
     * @param memberId the member ID
     * @return member earnings response with summary and details
     */
    MemberEarningsResponse getMemberEarnings(UUID memberId);

    /**
     * Gets all benefits distributed for a specific loan.
     *
     * @param loanId the loan ID
     * @return list of benefit responses
     */
    List<LoanBenefitResponse> getLoanBenefits(UUID loanId);

    /**
     * Gets all benefits with cursor pagination.
     *
     * @param cursor the cursor for pagination
     * @param limit the page size
     * @return list of benefit responses
     */
    List<LoanBenefitResponse> getAllBenefits(UUID cursor, int limit);

    /**
     * Recalculates and redistributes benefits for a loan when share balances change.
     * This marks previous benefits as distributed and creates new benefit records.
     *
     * @param loanId the loan ID
     * @param actor the actor performing the refresh
     * @return list of newly created benefit records
     */
    List<LoanBenefit> refreshBeneficiaries(UUID loanId, String actor);
}
