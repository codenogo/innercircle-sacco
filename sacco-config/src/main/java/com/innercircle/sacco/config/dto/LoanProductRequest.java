package com.innercircle.sacco.config.dto;

import com.innercircle.sacco.config.entity.InterestMethod;
import jakarta.validation.constraints.DecimalMin;
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
public class LoanProductRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Interest method is required")
    private InterestMethod interestMethod;

    @NotNull(message = "Annual interest rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Interest rate must be greater than 0")
    private BigDecimal annualInterestRate;

    @NotNull(message = "Max term months is required")
    @Min(value = 1, message = "Max term must be at least 1 month")
    private Integer maxTermMonths;

    @NotNull(message = "Min term months is required")
    @Min(value = 1, message = "Min term must be at least 1 month")
    private Integer minTermMonths;

    @NotNull(message = "Min amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Min amount must be greater than 0")
    private BigDecimal minAmount;

    @NotNull(message = "Max amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Max amount must be greater than 0")
    private BigDecimal maxAmount;

    @DecimalMin(value = "0.0", inclusive = true, message = "Contribution cap percent must be non-negative")
    private BigDecimal contributionCapPercent;

    @DecimalMin(value = "0.0", inclusive = true, message = "Pool cap amount must be non-negative")
    private BigDecimal poolCapAmount;

    private boolean rolloverEnabled;

    @Min(value = 0, message = "Max rollover months must be non-negative")
    private Integer maxRolloverMonths;

    @DecimalMin(value = "0.0", inclusive = true, message = "Rollover surcharge rate must be non-negative")
    private BigDecimal rolloverSurchargeRate;

    private boolean interestAccrualEnabled;

    private boolean requiresGuarantor;

    private boolean active;
}
