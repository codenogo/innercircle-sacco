package com.innercircle.sacco.investment.dto;

import com.innercircle.sacco.investment.entity.IncomeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordIncomeRequest(
        @NotNull(message = "Income type is required")
        IncomeType incomeType,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @NotNull(message = "Income date is required")
        LocalDate incomeDate,

        @Size(max = 100, message = "Reference number must be 100 characters or fewer")
        String referenceNumber,

        @Size(max = 1000, message = "Notes must be 1000 characters or fewer")
        String notes
) {
}
