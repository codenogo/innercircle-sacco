package com.innercircle.sacco.payout.dto;

import com.innercircle.sacco.payout.entity.Payout;
import com.innercircle.sacco.payout.entity.PayoutStatus;
import com.innercircle.sacco.payout.entity.PayoutType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayoutResponse(
        UUID id,
        UUID memberId,
        BigDecimal amount,
        PayoutType type,
        PayoutStatus status,
        String approvedBy,
        Instant processedAt,
        String referenceNumber,
        Instant createdAt,
        Instant updatedAt
) {
    public static PayoutResponse from(Payout payout) {
        return new PayoutResponse(
                payout.getId(),
                payout.getMemberId(),
                payout.getAmount(),
                payout.getType(),
                payout.getStatus(),
                payout.getApprovedBy(),
                payout.getProcessedAt(),
                payout.getReferenceNumber(),
                payout.getCreatedAt(),
                payout.getUpdatedAt()
        );
    }
}
