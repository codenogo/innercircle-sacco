package com.innercircle.sacco.contribution.dto;

import com.innercircle.sacco.contribution.entity.ContributionObligation;
import com.innercircle.sacco.contribution.entity.ContributionObligationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ContributionObligationResponse(
        UUID id,
        UUID memberId,
        UUID scheduleConfigId,
        LocalDate obligationMonth,
        LocalDate dueDate,
        BigDecimal grossAmount,
        BigDecimal paidAmount,
        BigDecimal penaltyAmount,
        ContributionObligationStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static ContributionObligationResponse fromEntity(ContributionObligation obligation) {
        return new ContributionObligationResponse(
                obligation.getId(),
                obligation.getMemberId(),
                obligation.getScheduleConfigId(),
                obligation.getObligationMonth(),
                obligation.getDueDate(),
                obligation.getGrossAmount(),
                obligation.getPaidAmount(),
                obligation.getPenaltyAmount(),
                obligation.getStatus(),
                obligation.getNotes(),
                obligation.getCreatedAt(),
                obligation.getUpdatedAt()
        );
    }
}
