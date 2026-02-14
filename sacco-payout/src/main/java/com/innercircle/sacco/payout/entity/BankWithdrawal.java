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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bank_withdrawals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankWithdrawal extends BaseEntity {

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 100)
    private String bankName;

    @Column(nullable = false, length = 50)
    private String accountNumber;

    @Column(length = 100)
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WithdrawalStatus status;

    private LocalDate transactionDate;

    @Column(nullable = false)
    private boolean reconciled;

    public BankWithdrawal(UUID memberId, BigDecimal amount, String bankName, String accountNumber) {
        this.memberId = memberId;
        this.amount = amount;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.status = WithdrawalStatus.PENDING;
        this.reconciled = false;
    }
}
