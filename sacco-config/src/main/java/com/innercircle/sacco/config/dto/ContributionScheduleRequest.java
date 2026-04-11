package com.innercircle.sacco.config.dto;

import com.innercircle.sacco.config.entity.ContributionScheduleConfig.Frequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionScheduleRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Frequency is required")
    private Frequency frequency;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Due day of month is required")
    @Min(value = 1, message = "Due day must be between 1 and 31")
    @Max(value = 31, message = "Due day must be between 1 and 31")
    private Integer dueDayOfMonth;

    @NotNull(message = "Grace period days is required")
    @Min(value = 0, message = "Grace period days must be non-negative")
    private Integer gracePeriodDays;

    private boolean mandatory;

    @NotNull(message = "Expected gross amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Expected gross amount must be non-negative")
    private BigDecimal expectedGrossAmount;

    private boolean penaltyEnabled;

    private boolean active;
}
