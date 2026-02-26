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
@Table(name = "member_exit_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberExitRequest extends BaseEntity {

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private LocalDate noticeDate;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MemberExitRequestStatus status = MemberExitRequestStatus.REQUESTED;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossSettlementAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal liabilityOffsetAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal exitFeeAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netSettlementAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer installmentCount = 2;

    @Column(nullable = false)
    private Integer installmentsProcessed = 0;

    @Column
    private LocalDate nextInstallmentDueDate;

    @Column(length = 1000)
    private String reviewNotes;

    @Column(length = 255)
    private String reviewedBy;

    @Column
    private Instant reviewedAt;
}
