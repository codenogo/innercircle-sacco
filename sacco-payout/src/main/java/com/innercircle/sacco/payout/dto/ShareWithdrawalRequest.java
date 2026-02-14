package com.innercircle.sacco.payout.dto;

import com.innercircle.sacco.payout.entity.ShareWithdrawal.ShareWithdrawalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record ShareWithdrawalRequest(
        @NotNull(message = "Member ID is required")
        UUID memberId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @NotNull(message = "Withdrawal type is required")
        ShareWithdrawalType withdrawalType,

        @NotNull(message = "Current share balance is required")
        BigDecimal currentShareBalance
) {
}
