package com.innercircle.sacco.payout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CashDisbursementRequest(
        @NotNull(message = "Member ID is required")
        UUID memberId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @NotBlank(message = "Received by is required")
        String receivedBy,

        @NotBlank(message = "Disbursed by is required")
        String disbursedBy,

        @NotBlank(message = "Receipt number is required")
        String receiptNumber,

        @NotNull(message = "Disbursement date is required")
        LocalDate disbursementDate
) {
}
