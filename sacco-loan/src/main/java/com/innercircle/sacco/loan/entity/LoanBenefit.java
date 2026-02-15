package com.innercircle.sacco.loan.entity;

import com.innercircle.sacco.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loan_benefits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanBenefit extends BaseEntity {

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private UUID loanId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal contributionSnapshot;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal benefitsRate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal earnedAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedEarnings;

    @Column(nullable = false)
    private boolean distributed = false;

    private Instant distributedAt;
}
