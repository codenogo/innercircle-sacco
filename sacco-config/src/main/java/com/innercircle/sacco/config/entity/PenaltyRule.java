package com.innercircle.sacco.config.entity;

import com.innercircle.sacco.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "penalty_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyRule extends BaseEntity {

    public enum PenaltyType {
        LATE_CONTRIBUTION,
        LOAN_DEFAULT
    }

    public enum CalculationMethod {
        FLAT,
        PERCENTAGE
    }

    @Column(nullable = false, length = 100)
    @NotBlank
    private String name;

    @Column(nullable = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    private PenaltyType penaltyType;

    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal rate;

    @Column(nullable = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    private CalculationMethod calculationMethod;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean compounding = false;
}
