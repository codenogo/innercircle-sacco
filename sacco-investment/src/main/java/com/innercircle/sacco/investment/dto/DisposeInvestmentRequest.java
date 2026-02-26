package com.innercircle.sacco.investment.dto;

import com.innercircle.sacco.investment.entity.DisposalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DisposeInvestmentRequest(
        @NotNull(message = "Disposal type is required")
        DisposalType disposalType,

        @NotNull(message = "Proceeds amount is required")
        @Positive(message = "Proceeds amount must be positive")
        BigDecimal proceedsAmount,

        @NotNull(message = "Fees are required")
        @PositiveOrZero(message = "Fees must be zero or positive")
        BigDecimal fees,

        @NotNull(message = "Disposal date is required")
        LocalDate disposalDate,

        @Positive(message = "Units redeemed must be positive")
        BigDecimal unitsRedeemed,

        @Size(max = 1000, message = "Notes must be 1000 characters or fewer")
        String notes
) {
}
