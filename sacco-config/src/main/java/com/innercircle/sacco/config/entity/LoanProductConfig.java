package com.innercircle.sacco.config.entity;

import com.innercircle.sacco.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "loan_product_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanProductConfig extends BaseEntity {

    @Column(nullable = false, length = 100)
    @NotBlank
    private String name;

    @Column(nullable = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    private InterestMethod interestMethod;

    @Column(nullable = false, precision = 5, scale = 2)
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal annualInterestRate;

    @Column(nullable = false)
    @NotNull
    @Min(1)
    private Integer maxTermMonths;

    @Column(nullable = false, precision = 15, scale = 2)
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal maxAmount;

    @Column(nullable = false)
    private boolean requiresGuarantor;

    @Column(nullable = false)
    private boolean active;
}
