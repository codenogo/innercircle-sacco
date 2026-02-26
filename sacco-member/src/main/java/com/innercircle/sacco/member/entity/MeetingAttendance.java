package com.innercircle.sacco.member.entity;

import com.innercircle.sacco.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meeting_attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeetingAttendance extends BaseEntity {

    @Column(nullable = false)
    private UUID meetingId;

    @Column(nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 20)
    private MeetingAttendanceStatus attendanceStatus;

    @Column
    private Instant arrivedAt;

    @Column(length = 500)
    private String remarks;
}
