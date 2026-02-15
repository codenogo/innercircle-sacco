package com.innercircle.sacco.loan.entity;

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
@Table(name = "loan_interest_history")
@Getter
@Setter
@NoArgsConstructor
public class LoanInterestHistory extends BaseEntity {

    @Column(nullable = false)
    private UUID loanId;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private LocalDate accrualDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal interestAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingBalanceSnapshot;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal cumulativeInterestAccrued;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterestEventType eventType;

    @Column(length = 500)
    private String description;
}
