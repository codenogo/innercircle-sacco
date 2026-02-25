package com.innercircle.sacco.investment.dto;

import com.innercircle.sacco.investment.entity.IncomeType;
import com.innercircle.sacco.investment.entity.InvestmentIncome;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InvestmentIncomeResponse(
        UUID id,
        UUID investmentId,
        IncomeType incomeType,
        BigDecimal amount,
        LocalDate incomeDate,
        String referenceNumber,
        String notes,
        String recordedBy,
        Instant createdAt
) {

    public static InvestmentIncomeResponse from(InvestmentIncome income) {
        return new InvestmentIncomeResponse(
                income.getId(),
                income.getInvestmentId(),
                income.getIncomeType(),
                income.getAmount(),
                income.getIncomeDate(),
                income.getReferenceNumber(),
                income.getNotes(),
                income.getCreatedBy(),
                income.getCreatedAt()
        );
    }
}
