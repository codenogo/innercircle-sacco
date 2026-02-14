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

    @NotNull(message = "Max amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Max amount must be greater than 0")
    private BigDecimal maxAmount;

    private boolean requiresGuarantor;

    private boolean active;
}
