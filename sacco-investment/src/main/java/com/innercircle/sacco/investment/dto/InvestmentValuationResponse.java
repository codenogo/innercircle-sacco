package com.innercircle.sacco.investment.dto;

import com.innercircle.sacco.investment.entity.InvestmentValuation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InvestmentValuationResponse(
        UUID id,
        UUID investmentId,
        BigDecimal marketValue,
        BigDecimal navPerUnit,
        LocalDate valuationDate,
        String source,
        Instant createdAt
) {

    public static InvestmentValuationResponse from(InvestmentValuation valuation) {
        return new InvestmentValuationResponse(
                valuation.getId(),
                valuation.getInvestmentId(),
                valuation.getMarketValue(),
                valuation.getNavPerUnit(),
                valuation.getValuationDate(),
                valuation.getSource(),
                valuation.getCreatedAt()
        );
    }
}
