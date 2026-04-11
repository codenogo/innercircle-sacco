package com.innercircle.sacco.member.entity;

import com.innercircle.sacco.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "welfare_benefit_catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WelfareBenefitCatalog extends BaseEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String eventCode;

    @Column(nullable = false, length = 200)
    private String eventName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal defaultAmount;

    @Column
    private Integer maxClaimsPerYear;

    @Column(nullable = false)
    private boolean active = true;
}
