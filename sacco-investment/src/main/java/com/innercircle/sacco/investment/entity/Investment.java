package com.innercircle.sacco.investment.entity;

import com.innercircle.sacco.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "investments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Investment extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String referenceNumber;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private InvestmentType investmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvestmentStatus status = InvestmentStatus.PROPOSED;

    @Column(nullable = false, length = 255)
    private String institution;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal faceValue;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal purchasePrice;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentValue;

    @Column(nullable = false, precision = 9, scale = 4)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private LocalDate purchaseDate;

    private LocalDate maturityDate;

    @Column(precision = 19, scale = 4)
    private BigDecimal units;

    @Column(precision = 19, scale = 4)
    private BigDecimal navPerUnit;

    @Column(length = 2000)
    private String notes;

    @Column(length = 100)
    private String approvedBy;

    private Instant approvedAt;

    @Column(length = 100)
    private String rejectedBy;

    private Instant rejectedAt;

    @Column(length = 500)
    private String rejectionReason;

    public Investment(String referenceNumber,
                      String name,
                      InvestmentType investmentType,
                      String institution,
                      BigDecimal faceValue,
                      BigDecimal purchasePrice,
                      BigDecimal currentValue,
                      BigDecimal interestRate,
                      LocalDate purchaseDate,
                      LocalDate maturityDate,
                      BigDecimal units,
                      BigDecimal navPerUnit,
                      String notes) {
        this.referenceNumber = referenceNumber;
        this.name = name;
        this.investmentType = investmentType;
        this.status = InvestmentStatus.PROPOSED;
        this.institution = institution;
        this.faceValue = faceValue;
        this.purchasePrice = purchasePrice;
        this.currentValue = currentValue;
        this.interestRate = interestRate;
        this.purchaseDate = purchaseDate;
        this.maturityDate = maturityDate;
        this.units = units;
        this.navPerUnit = navPerUnit;
        this.notes = notes;
    }
}
