package com.innercircle.sacco.loan.entity;

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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "loan_repayments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanRepayment extends BaseEntity {

    @Column(nullable = false)
    private UUID loanId;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principalPortion;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal interestPortion;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal penaltyPortion = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate repaymentDate;

    @Column(nullable = false, length = 100)
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RepaymentStatus status = RepaymentStatus.CONFIRMED;
}
