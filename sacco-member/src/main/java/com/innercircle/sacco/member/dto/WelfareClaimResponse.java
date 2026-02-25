package com.innercircle.sacco.member.dto;

import com.innercircle.sacco.member.entity.WelfareClaim;
import com.innercircle.sacco.member.entity.WelfareClaimStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WelfareClaimResponse(
        UUID id,
        UUID memberId,
        UUID beneficiaryId,
        UUID benefitCatalogId,
        String eventCode,
        LocalDate eventDate,
        BigDecimal requestedAmount,
        BigDecimal approvedAmount,
        WelfareClaimStatus status,
        String decisionSource,
        String meetingReference,
        LocalDate decisionDate,
        String decisionNotes,
        String reviewedBy,
        UUID processedPayoutId,
        Instant createdAt,
        Instant updatedAt
) {
    public static WelfareClaimResponse fromEntity(WelfareClaim claim) {
        return new WelfareClaimResponse(
                claim.getId(),
                claim.getMemberId(),
                claim.getBeneficiaryId(),
                claim.getBenefitCatalogId(),
                claim.getEventCode(),
                claim.getEventDate(),
                claim.getRequestedAmount(),
                claim.getApprovedAmount(),
                claim.getStatus(),
                claim.getDecisionSource(),
                claim.getMeetingReference(),
                claim.getDecisionDate(),
                claim.getDecisionNotes(),
                claim.getReviewedBy(),
                claim.getProcessedPayoutId(),
                claim.getCreatedAt(),
                claim.getUpdatedAt()
        );
    }
}
