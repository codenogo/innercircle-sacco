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

@Entity
@Table(name = "petty_cash_vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PettyCashVoucher extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String referenceNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 500)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PettyCashExpenseType expenseType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PettyCashVoucherStatus status = PettyCashVoucherStatus.SUBMITTED;

    @Column(nullable = false)
    private LocalDate requestDate;

    @Column(length = 100)
    private String approvedBy;

    @Column(length = 100)
    private String disbursedBy;

    @Column(length = 100)
    private String settledBy;

    @Column(length = 100)
    private String rejectedBy;

    private Instant disbursedAt;

    private Instant settledAt;

    private Instant rejectedAt;

    @Column(length = 100)
    private String receiptNumber;

    @Column(length = 500)
    private String rejectionReason;

    @Column(length = 500)
    private String notes;

    public PettyCashVoucher(String referenceNumber,
                            BigDecimal amount,
                            String purpose,
                            PettyCashExpenseType expenseType,
                            LocalDate requestDate,
                            String notes) {
        this.referenceNumber = referenceNumber;
        this.amount = amount;
        this.purpose = purpose;
        this.expenseType = expenseType;
        this.status = PettyCashVoucherStatus.SUBMITTED;
        this.requestDate = requestDate;
        this.notes = notes;
    }
}
