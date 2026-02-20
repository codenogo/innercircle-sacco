package com.innercircle.sacco.loan.entity;

import com.innercircle.sacco.common.model.BaseEntity;
import com.innercircle.sacco.config.entity.InterestMethod;
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
import java.util.UUID;

@Entity
@Table(name = "loan_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplication extends BaseEntity {

    @Column(nullable = false)
    private UUID memberId;

    @Column(name = "loan_number", nullable = false, unique = true, length = 50)
    private String loanNumber;

    private UUID loanProductId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private Integer termMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InterestMethod interestMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.PENDING;

    @Column(length = 500)
    private String purpose;

    private UUID approvedBy;

    private Instant approvedAt;

    private Instant disbursedAt;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalRepaid = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalInterestAccrued = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalInterestPaid = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPenalties = BigDecimal.ZERO;
}
