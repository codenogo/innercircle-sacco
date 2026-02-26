package com.innercircle.sacco.investment.entity;

import com.innercircle.sacco.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "investment_valuations")
@Getter
@Setter
@NoArgsConstructor
public class InvestmentValuation extends BaseEntity {

    @Column(nullable = false)
    private UUID investmentId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal marketValue;

    @Column(precision = 19, scale = 4)
    private BigDecimal navPerUnit;

    @Column(nullable = false)
    private LocalDate valuationDate;

    @Column(nullable = false, length = 255)
    private String source;

    public InvestmentValuation(UUID investmentId,
                               BigDecimal marketValue,
                               BigDecimal navPerUnit,
                               LocalDate valuationDate,
                               String source) {
        this.investmentId = investmentId;
        this.marketValue = marketValue;
        this.navPerUnit = navPerUnit;
        this.valuationDate = valuationDate;
        this.source = source;
    }
}
