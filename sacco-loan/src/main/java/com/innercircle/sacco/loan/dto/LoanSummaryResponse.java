package com.innercircle.sacco.loan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class LoanSummaryResponse {

    private UUID memberId;
    private Integer totalLoans;
    private Integer activeLoans;
    private Integer closedLoans;
    private BigDecimal totalBorrowed;
    private BigDecimal totalRepaid;
    private BigDecimal totalOutstanding;
    private List<LoanResponse> loans;
}
