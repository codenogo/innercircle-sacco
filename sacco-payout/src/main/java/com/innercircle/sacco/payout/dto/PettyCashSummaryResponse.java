package com.innercircle.sacco.payout.dto;

import java.math.BigDecimal;

public record PettyCashSummaryResponse(
        long totalCount,
        long submittedCount,
        long approvedCount,
        long disbursedCount,
        long settledCount,
        long rejectedCount,
        BigDecimal disbursedAmount,
        BigDecimal settledAmount,
        BigDecimal outstandingAmount
) {
}
