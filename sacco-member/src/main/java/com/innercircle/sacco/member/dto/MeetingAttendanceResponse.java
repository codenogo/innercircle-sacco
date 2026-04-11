package com.innercircle.sacco.member.dto;

import com.innercircle.sacco.member.entity.MeetingAttendance;
import com.innercircle.sacco.member.entity.MeetingAttendanceStatus;

import java.time.Instant;
import java.util.UUID;

public record MeetingAttendanceResponse(
        UUID id,
        UUID meetingId,
        UUID memberId,
        MeetingAttendanceStatus attendanceStatus,
        Instant arrivedAt,
        String remarks,
        Instant createdAt,
        Instant updatedAt
) {
    public static MeetingAttendanceResponse fromEntity(MeetingAttendance attendance) {
        return new MeetingAttendanceResponse(
                attendance.getId(),
                attendance.getMeetingId(),
                attendance.getMemberId(),
                attendance.getAttendanceStatus(),
                attendance.getArrivedAt(),
                attendance.getRemarks(),
                attendance.getCreatedAt(),
                attendance.getUpdatedAt()
        );
    }
}