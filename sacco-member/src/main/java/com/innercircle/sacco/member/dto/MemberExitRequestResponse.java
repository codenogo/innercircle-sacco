package com.innercircle.sacco.member.dto;

import com.innercircle.sacco.member.entity.MemberExitRequest;
import com.innercircle.sacco.member.entity.MemberExitRequestStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MemberExitRequestResponse(
        UUID id,
        UUID memberId,
        LocalDate noticeDate,
        LocalDate effectiveDate,
        MemberExitRequestStatus status,
        BigDecimal grossSettlementAmount,
        BigDecimal liabilityOffsetAmount,
        BigDecimal exitFeeAmount,
        BigDecimal netSettlementAmount,
        Integer installmentCount,
        Integer installmentsProcessed,
        LocalDate nextInstallmentDueDate,
        String reviewNotes,
        String reviewedBy,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static MemberExitRequestResponse fromEntity(MemberExitRequest request) {
        return new MemberExitRequestResponse(
                request.getId(),
                request.getMemberId(),
                request.getNoticeDate(),
                request.getEffectiveDate(),
                request.getStatus(),
                request.getGrossSettlementAmount(),
                request.getLiabilityOffsetAmount(),
                request.getExitFeeAmount(),
                request.getNetSettlementAmount(),
                request.getInstallmentCount(),
                request.getInstallmentsProcessed(),
                request.getNextInstallmentDueDate(),
                request.getReviewNotes(),
                request.getReviewedBy(),
                request.getReviewedAt(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
