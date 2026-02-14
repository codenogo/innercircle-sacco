package com.innercircle.sacco.payout.dto;

import com.innercircle.sacco.payout.entity.PayoutType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record PayoutRequest(
        @NotNull(message = "Member ID is required")
        UUID memberId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @NotNull(message = "Payout type is required")
        PayoutType type
) {
}
