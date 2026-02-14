package com.innercircle.sacco.payout.dto;

import com.innercircle.sacco.payout.entity.BankWithdrawal;
import com.innercircle.sacco.payout.entity.WithdrawalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BankWithdrawalResponse(
        UUID id,
        UUID memberId,
        BigDecimal amount,
        String bankName,
        String accountNumber,
        String referenceNumber,
        WithdrawalStatus status,
        LocalDate transactionDate,
        boolean reconciled,
        Instant createdAt,
        Instant updatedAt
) {
    public static BankWithdrawalResponse from(BankWithdrawal withdrawal) {
        return new BankWithdrawalResponse(
                withdrawal.getId(),
                withdrawal.getMemberId(),
                withdrawal.getAmount(),
                withdrawal.getBankName(),
                withdrawal.getAccountNumber(),
                withdrawal.getReferenceNumber(),
                withdrawal.getStatus(),
                withdrawal.getTransactionDate(),
                withdrawal.isReconciled(),
                withdrawal.getCreatedAt(),
                withdrawal.getUpdatedAt()
        );
    }
}
