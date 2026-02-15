package com.innercircle.sacco.loan.dto;

import com.innercircle.sacco.loan.entity.LoanBenefit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanBenefitResponse {

    private UUID id;
    private UUID memberId;
    private UUID loanId;
    private BigDecimal contributionSnapshot;
    private BigDecimal benefitsRate;
    private BigDecimal earnedAmount;
    private BigDecimal expectedEarnings;
    private boolean distributed;
    private Instant distributedAt;
    private Instant createdAt;

    public static LoanBenefitResponse from(LoanBenefit benefit) {
        return LoanBenefitResponse.builder()
                .id(benefit.getId())
                .memberId(benefit.getMemberId())
                .loanId(benefit.getLoanId())
                .contributionSnapshot(benefit.getContributionSnapshot())
                .benefitsRate(benefit.getBenefitsRate())
                .earnedAmount(benefit.getEarnedAmount())
                .expectedEarnings(benefit.getExpectedEarnings())
                .distributed(benefit.isDistributed())
                .distributedAt(benefit.getDistributedAt())
                .createdAt(benefit.getCreatedAt())
                .build();
    }
}
