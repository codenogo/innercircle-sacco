package com.innercircle.sacco.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialSummaryResponse(
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal totalContributions,
        BigDecimal totalLoansDisbursed,
        BigDecimal totalRepayments,
        BigDecimal totalPayouts,
        BigDecimal totalPenaltiesCollected,
        BigDecimal netPosition,
        long activeMemberCount,
        long activeLoansCount,
        BigDecimal outstandingLoanBalance
) {}
