package com.innercircle.sacco.member.dto;

import com.innercircle.sacco.member.entity.MeetingFine;
import com.innercircle.sacco.member.entity.MeetingFineType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MeetingFineResponse(
        UUID id,
        UUID meetingId,
        UUID attendanceId,
        UUID memberId,
        MeetingFineType fineType,
        UUID baseFineId,
        BigDecimal amount,
        LocalDate dueDate,
        boolean settled,
        Instant settledAt,
        boolean waived,
        String waivedBy,
        Instant waivedAt,
        String waivedReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static MeetingFineResponse fromEntity(MeetingFine fine) {
        return new MeetingFineResponse(
                fine.getId(),
                fine.getMeetingId(),
                fine.getAttendanceId(),
                fine.getMemberId(),
                fine.getFineType(),
                fine.getBaseFineId(),
                fine.getAmount(),
                fine.getDueDate(),
                fine.isSettled(),
                fine.getSettledAt(),
                fine.isWaived(),
                fine.getWaivedBy(),
                fine.getWaivedAt(),
                fine.getWaivedReason(),
                fine.getCreatedAt(),
                fine.getUpdatedAt()
        );
    }
}
