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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "welfare_claims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WelfareClaim extends BaseEntity {

    @Column(nullable = false)
    private UUID memberId;

    @Column
    private UUID beneficiaryId;

    @Column
    private UUID benefitCatalogId;

    @Column(nullable = false, length = 80)
    private String eventCode;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal requestedAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal approvedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WelfareClaimStatus status = WelfareClaimStatus.SUBMITTED;

    @Column(length = 80)
    private String decisionSource;

    @Column(length = 120)
    private String meetingReference;

    @Column
    private LocalDate decisionDate;

    @Column(length = 1000)
    private String decisionNotes;

    @Column(length = 255)
    private String reviewedBy;

    @Column
    private UUID processedPayoutId;
}
