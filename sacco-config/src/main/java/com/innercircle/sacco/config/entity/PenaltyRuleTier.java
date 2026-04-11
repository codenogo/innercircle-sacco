package com.innercircle.sacco.config.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.innercircle.sacco.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "penalty_rule_tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyRuleTier extends BaseEntity {

    public enum PenaltyFrequency {
        DAILY,
        MONTHLY,
        ONCE
    }

    @ManyToOne(optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    @JsonBackReference
    private PenaltyRule rule;

    @Column(nullable = false)
    @NotNull
    @Min(1)
    private Integer sequence;

    @Column(nullable = false)
    @NotNull
    @Min(1)
    private Integer startOverdueDay;

    @Column
    @Min(1)
    private Integer endOverdueDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull
    private PenaltyFrequency frequency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull
    private PenaltyRule.CalculationMethod calculationMethod;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal rate;

    @Column
    @Min(1)
    private Integer maxApplications;

    @Column(nullable = false)
    private boolean active = true;
}
