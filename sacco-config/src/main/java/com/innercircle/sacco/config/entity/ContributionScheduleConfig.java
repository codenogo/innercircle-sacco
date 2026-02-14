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
@Table(name = "contribution_schedule_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContributionScheduleConfig extends BaseEntity {

    public enum Frequency {
        WEEKLY,
        MONTHLY
    }

    @Column(nullable = false, length = 100)
    @NotBlank
    private String name;

    @Column(nullable = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    @Column(nullable = false, precision = 15, scale = 2)
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private boolean penaltyEnabled;

    @Column(nullable = false)
    private boolean active;
}
