package com.innercircle.sacco.investment.dto;

import jakarta.validation.constraints.Size;

public record RejectInvestmentRequest(
        @Size(max = 500, message = "Reason must be 500 characters or fewer")
        String reason
) {
}
