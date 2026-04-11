package com.innercircle.sacco.investment.dto;

import com.innercircle.sacco.investment.entity.InvestmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateInvestmentRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be 255 characters or fewer")
        String name,

        @NotNull(message = "Investment type is required")
        InvestmentType investmentType,

        @NotBlank(message = "Institution is required")
        @Size(max = 255, message = "Institution must be 255 characters or fewer")
        String institution,

        @NotNull(message = "Face value is required")
        @Positive(message = "Face value must be positive")
        BigDecimal faceValue,

        @NotNull(message = "Purchase price is required")
        @Positive(message = "Purchase price must be positive")
        BigDecimal purchasePrice,

        @NotNull(message = "Interest rate is required")
        @PositiveOrZero(message = "Interest rate must be zero or positive")
        BigDecimal interestRate,

        @NotNull(message = "Purchase date is required")
        LocalDate purchaseDate,

        LocalDate maturityDate,

        @Positive(message = "Units must be positive")
        BigDecimal units,

        @Positive(message = "NAV per unit must be positive")
        BigDecimal navPerUnit,

        @Size(max = 2000, message = "Notes must be 2000 characters or fewer")
        String notes
) {
}
