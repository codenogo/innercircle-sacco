package com.innercircle.sacco.payout.entity;

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
import java.util.UUID;

@Entity
@Table(name = "cash_disbursements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CashDisbursement extends BaseEntity {

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 100)
    private String receivedBy;

    @Column(nullable = false, length = 100)
    private String disbursedBy;

    @Column(length = 100)
    private String signoffBy;

    @Column(nullable = false, length = 50)
    private String receiptNumber;

    @Column(nullable = false)
    private LocalDate disbursementDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CashDisbursementStatus status = CashDisbursementStatus.PENDING;

    @Column(length = 255)
    private String approvedBy;

    @Column
    private Instant approvedAt;

    public CashDisbursement(UUID memberId, BigDecimal amount, String receivedBy,
                           String disbursedBy, String receiptNumber, LocalDate disbursementDate) {
        this.memberId = memberId;
        this.amount = amount;
        this.receivedBy = receivedBy;
        this.disbursedBy = disbursedBy;
        this.receiptNumber = receiptNumber;
        this.disbursementDate = disbursementDate;
        this.status = CashDisbursementStatus.PENDING;
    }
}
