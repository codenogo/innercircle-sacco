package com.innercircle.sacco.payout.dto;

import com.innercircle.sacco.payout.entity.ShareWithdrawal;
import com.innercircle.sacco.payout.entity.ShareWithdrawal.ShareWithdrawalStatus;
import com.innercircle.sacco.payout.entity.ShareWithdrawal.ShareWithdrawalType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShareWithdrawalResponse(
        UUID id,
        UUID memberId,
        BigDecimal amount,
        ShareWithdrawalType withdrawalType,
        ShareWithdrawalStatus status,
        BigDecimal currentShareBalance,
        BigDecimal newShareBalance,
        String approvedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static ShareWithdrawalResponse from(ShareWithdrawal withdrawal) {
        return new ShareWithdrawalResponse(
                withdrawal.getId(),
                withdrawal.getMemberId(),
                withdrawal.getAmount(),
                withdrawal.getWithdrawalType(),
                withdrawal.getStatus(),
                withdrawal.getCurrentShareBalance(),
                withdrawal.getNewShareBalance(),
                withdrawal.getApprovedBy(),
                withdrawal.getCreatedAt(),
                withdrawal.getUpdatedAt()
        );
    }
}
