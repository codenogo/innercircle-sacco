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
import java.util.UUID;

@Entity
@Table(name = "share_withdrawals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShareWithdrawal extends BaseEntity {

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ShareWithdrawalType withdrawalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShareWithdrawalStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentShareBalance;

    @Column(precision = 19, scale = 2)
    private BigDecimal newShareBalance;

    @Column(length = 100)
    private String approvedBy;

    public ShareWithdrawal(UUID memberId, BigDecimal amount, ShareWithdrawalType withdrawalType,
                          BigDecimal currentShareBalance) {
        this.memberId = memberId;
        this.amount = amount;
        this.withdrawalType = withdrawalType;
        this.currentShareBalance = currentShareBalance;
        this.status = ShareWithdrawalStatus.PENDING;
    }

    public enum ShareWithdrawalType {
        PARTIAL,
        FULL
    }

    public enum ShareWithdrawalStatus {
        PENDING,
        APPROVED,
        PROCESSED
    }
}
