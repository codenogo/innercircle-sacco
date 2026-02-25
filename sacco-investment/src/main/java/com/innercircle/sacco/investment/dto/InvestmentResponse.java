package com.innercircle.sacco.investment.dto;

import com.innercircle.sacco.investment.entity.Investment;
import com.innercircle.sacco.investment.entity.InvestmentStatus;
import com.innercircle.sacco.investment.entity.InvestmentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InvestmentResponse(
        UUID id,
        String referenceNumber,
        String name,
        InvestmentType investmentType,
        InvestmentStatus status,
        String institution,
        BigDecimal faceValue,
        BigDecimal purchasePrice,
        BigDecimal currentValue,
        BigDecimal interestRate,
        LocalDate purchaseDate,
        LocalDate maturityDate,
        BigDecimal units,
        BigDecimal navPerUnit,
        String notes,
        String approvedBy,
        Instant approvedAt,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {

    public static InvestmentResponse from(Investment investment) {
        return new InvestmentResponse(
                investment.getId(),
                investment.getReferenceNumber(),
                investment.getName(),
                investment.getInvestmentType(),
                investment.getStatus(),
                investment.getInstitution(),
                investment.getFaceValue(),
                investment.getPurchasePrice(),
                investment.getCurrentValue(),
                investment.getInterestRate(),
                investment.getPurchaseDate(),
                investment.getMaturityDate(),
                investment.getUnits(),
                investment.getNavPerUnit(),
                investment.getNotes(),
                investment.getApprovedBy(),
                investment.getApprovedAt(),
                investment.getCreatedBy(),
                investment.getCreatedAt(),
                investment.getUpdatedAt()
        );
    }
}
