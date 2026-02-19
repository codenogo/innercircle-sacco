package com.innercircle.sacco.payout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SettlePettyCashVoucherRequest(
        @NotBlank(message = "Receipt number is required")
        @Size(max = 100, message = "Receipt number must be 100 characters or fewer")
        String receiptNumber,

        @Size(max = 500, message = "Notes must be 500 characters or fewer")
        String notes
) {
}
