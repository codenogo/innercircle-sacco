package com.innercircle.sacco.investment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordValuationRequest(
        @Positive(message = "Market value must be positive")
        BigDecimal marketValue,

        @Positive(message = "NAV per unit must be positive")
        BigDecimal navPerUnit,

        @NotNull(message = "Valuation date is required")
        LocalDate valuationDate,

        @NotBlank(message = "Source is required")
        @Size(max = 255, message = "Source must be 255 characters or fewer")
        String source
) {
}
