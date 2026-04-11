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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "meeting_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeetingSession extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private LocalDate meetingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MeetingStatus status = MeetingStatus.OPEN;

    @Column(nullable = false)
    private Integer lateThresholdMinutes;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal absenceFineAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal lateFineAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unpaidDailyPenaltyAmount;

    @Column
    private Instant finalizedAt;

    @Column(length = 1000)
    private String notes;
}
