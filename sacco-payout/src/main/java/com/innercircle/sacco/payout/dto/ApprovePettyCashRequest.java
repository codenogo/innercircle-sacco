package com.innercircle.sacco.payout.dto;

import jakarta.validation.constraints.Size;

public record ApprovePettyCashRequest(
        @Size(max = 500, message = "Override reason must be 500 characters or fewer")
        String overrideReason
) {
}
