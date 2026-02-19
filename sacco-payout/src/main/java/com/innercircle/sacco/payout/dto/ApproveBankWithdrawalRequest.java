package com.innercircle.sacco.payout.dto;

import jakarta.validation.constraints.Size;

public record ApproveBankWithdrawalRequest(
        @Size(max = 500, message = "Override reason must not exceed 500 characters")
        String overrideReason
) {
}
