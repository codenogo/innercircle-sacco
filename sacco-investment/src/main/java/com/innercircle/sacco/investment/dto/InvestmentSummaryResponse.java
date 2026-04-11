package com.innercircle.sacco.investment.dto;

import com.innercircle.sacco.investment.entity.InvestmentType;

import java.math.BigDecimal;
import java.util.List;

public record InvestmentSummaryResponse(
        BigDecimal totalInvested,
        BigDecimal currentValue,
        BigDecimal unrealisedGain,
        BigDecimal incomeYtd,
        long activeCount,
        long maturedCount,
        long proposedCount,
        long closedCount,
        List<InvestmentTypeAllocation> byType
) {

    public record InvestmentTypeAllocation(
            InvestmentType type,
            BigDecimal amount,
            BigDecimal percentage
    ) {
    }
}
