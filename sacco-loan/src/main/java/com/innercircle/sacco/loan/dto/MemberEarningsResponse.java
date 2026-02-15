package com.innercircle.sacco.loan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberEarningsResponse {

    private UUID memberId;
    private BigDecimal totalEarnings;
    private BigDecimal distributedEarnings;
    private BigDecimal pendingEarnings;
    private int totalBenefits;
    private int distributedCount;
    private int pendingCount;
    private List<LoanBenefitResponse> benefits;
}
