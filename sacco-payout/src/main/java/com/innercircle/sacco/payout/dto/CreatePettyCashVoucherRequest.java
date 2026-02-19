package com.innercircle.sacco.payout.dto;

import com.innercircle.sacco.payout.entity.PettyCashExpenseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePettyCashVoucherRequest(
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @NotBlank(message = "Purpose is required")
        @Size(max = 500, message = "Purpose must be 500 characters or fewer")
        String purpose,

        @NotNull(message = "Expense type is required")
        PettyCashExpenseType expenseType,

        LocalDate requestDate,

        @Size(max = 500, message = "Notes must be 500 characters or fewer")
        String notes
) {
}
