package com.innercircle.sacco.member.dto;

import com.innercircle.sacco.member.entity.MeetingSession;
import com.innercircle.sacco.member.entity.MeetingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MeetingResponse(
        UUID id,
        String title,
        LocalDate meetingDate,
        MeetingStatus status,
        Integer lateThresholdMinutes,
        BigDecimal absenceFineAmount,
        BigDecimal lateFineAmount,
        BigDecimal unpaidDailyPenaltyAmount,
        Instant finalizedAt,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static MeetingResponse fromEntity(MeetingSession session) {
        return new MeetingResponse(
                session.getId(),
                session.getTitle(),
                session.getMeetingDate(),
                session.getStatus(),
                session.getLateThresholdMinutes(),
                session.getAbsenceFineAmount(),
                session.getLateFineAmount(),
                session.getUnpaidDailyPenaltyAmount(),
                session.getFinalizedAt(),
                session.getNotes(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
