package com.innercircle.sacco.investment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RollOverRequest(
        @NotNull(message = "New maturity date is required")
        LocalDate newMaturityDate,

        @NotNull(message = "New interest rate is required")
        @PositiveOrZero(message = "New interest rate must be zero or positive")
        BigDecimal newInterestRate,

        @Size(max = 1000, message = "Notes must be 1000 characters or fewer")
        String notes
) {
}
