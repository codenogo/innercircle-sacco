package com.innercircle.sacco.config.dto;

import com.innercircle.sacco.config.entity.ContributionScheduleConfig.Frequency;
import jakarta.validation.constraints.DecimalMin;
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

    private boolean penaltyEnabled;

    private boolean active;
}
