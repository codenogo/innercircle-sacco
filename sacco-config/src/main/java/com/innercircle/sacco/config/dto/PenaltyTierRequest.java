package com.innercircle.sacco.config.dto;

import com.innercircle.sacco.config.entity.PenaltyRule;
import com.innercircle.sacco.config.entity.PenaltyRuleTier;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class PenaltyTierRequest {

    @NotNull(message = "Tier sequence is required")
    @Min(value = 1, message = "Tier sequence must be >= 1")
    private Integer sequence;

    @NotNull(message = "Start overdue day is required")
    @Min(value = 1, message = "Start overdue day must be >= 1")
    private Integer startOverdueDay;

    @Min(value = 1, message = "End overdue day must be >= 1")
    private Integer endOverdueDay;

    @NotNull(message = "Tier frequency is required")
    private PenaltyRuleTier.PenaltyFrequency frequency;

    @NotNull(message = "Tier calculation method is required")
    private PenaltyRule.CalculationMethod calculationMethod;

    @NotNull(message = "Tier rate is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Tier rate must be non-negative")
    private BigDecimal rate;

    @Min(value = 1, message = "Max applications must be >= 1")
    private Integer maxApplications;

    private boolean active = true;
}
