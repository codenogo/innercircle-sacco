package com.innercircle.sacco.member.dto;

import com.innercircle.sacco.member.entity.MeetingAttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class MeetingAttendanceEntryRequest {
    @NotNull
    private UUID memberId;

    @NotNull
    private MeetingAttendanceStatus attendanceStatus;

    private Instant arrivedAt;
    private String remarks;
}
