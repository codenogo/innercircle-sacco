package com.innercircle.sacco.payout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record BankWithdrawalRequest(
        @NotNull(message = "Member ID is required")
        UUID memberId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @NotBlank(message = "Bank name is required")
        String bankName,

        @NotBlank(message = "Account number is required")
        String accountNumber
) {
}
