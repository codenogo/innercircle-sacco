package com.innercircle.sacco.investment.entity;

import com.innercircle.sacco.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "investment_income")
@Getter
@Setter
@NoArgsConstructor
public class InvestmentIncome extends BaseEntity {

    @Column(nullable = false)
    private UUID investmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncomeType incomeType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate incomeDate;

    @Column(length = 100)
    private String referenceNumber;

    @Column(length = 1000)
    private String notes;

    public InvestmentIncome(UUID investmentId,
                            IncomeType incomeType,
                            BigDecimal amount,
                            LocalDate incomeDate,
                            String referenceNumber,
                            String notes) {
        this.investmentId = investmentId;
        this.incomeType = incomeType;
        this.amount = amount;
        this.incomeDate = incomeDate;
        this.referenceNumber = referenceNumber;
        this.notes = notes;
    }
}
