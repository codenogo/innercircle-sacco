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
import java.util.UUID;

@Entity
@Table(name = "meeting_fines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeetingFine extends BaseEntity {

    @Column(nullable = false)
    private UUID meetingId;

    @Column
    private UUID attendanceId;

    @Column(nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MeetingFineType fineType;

    @Column
    private UUID baseFineId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private boolean settled = false;

    @Column
    private Instant settledAt;

    @Column(nullable = false)
    private boolean waived = false;

    @Column(length = 255)
    private String waivedBy;

    @Column
    private Instant waivedAt;

    @Column(length = 500)
    private String waivedReason;
}
