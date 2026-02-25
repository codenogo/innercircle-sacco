package com.innercircle.sacco.contribution.dto;

import com.innercircle.sacco.contribution.entity.ContributionPenalty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ContributionPenaltyResponse(
        UUID id,
        UUID memberId,
        UUID contributionId,
        UUID obligationId,
        UUID ruleTierId,
        BigDecimal amount,
        String reason,
        String penaltyCode,
        LocalDate penaltyDate,
        boolean settled,
        Instant settledAt,
        boolean waived,
        String waivedReason,
        String waivedBy,
        Instant waivedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static ContributionPenaltyResponse fromEntity(ContributionPenalty penalty) {
        return new ContributionPenaltyResponse(
                penalty.getId(),
                penalty.getMemberId(),
                penalty.getContributionId(),
                penalty.getObligationId(),
                penalty.getRuleTierId(),
                penalty.getAmount(),
                penalty.getReason(),
                penalty.getPenaltyCode(),
                penalty.getPenaltyDate(),
                penalty.isSettled(),
                penalty.getSettledAt(),
                penalty.isWaived(),
                penalty.getWaivedReason(),
                penalty.getWaivedBy(),
                penalty.getWaivedAt(),
                penalty.getCreatedAt(),
                penalty.getUpdatedAt()
        );
    }
}
