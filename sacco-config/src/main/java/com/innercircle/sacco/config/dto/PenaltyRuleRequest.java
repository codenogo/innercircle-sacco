package com.innercircle.sacco.config.dto;

import com.innercircle.sacco.config.entity.PenaltyRule.CalculationMethod;
import com.innercircle.sacco.config.entity.PenaltyRule.PenaltyType;
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
public class PenaltyRuleRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Penalty type is required")
    private PenaltyType penaltyType;

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Rate must be non-negative")
    private BigDecimal rate;

    @NotNull(message = "Calculation method is required")
    private CalculationMethod calculationMethod;

    private boolean active;
}
