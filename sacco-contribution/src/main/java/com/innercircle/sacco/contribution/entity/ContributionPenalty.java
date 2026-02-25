package com.innercircle.sacco.contribution.entity;

import com.innercircle.sacco.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a penalty applied to a member for late or missing contributions.
 * Penalties can be waived by authorized personnel.
 */
@Entity
@Table(name = "contribution_penalties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContributionPenalty extends BaseEntity {

    @Column(nullable = false)
    private UUID memberId;

    @Column
    private UUID contributionId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, length = 120, unique = true)
    private String penaltyCode;

    @Column(nullable = false)
    private LocalDate penaltyDate;

    @Column
    private UUID ruleTierId;

    @Column
    private UUID obligationId;

    @Column(nullable = false)
    private boolean waived = false;

    @Column(length = 500)
    private String waivedReason;

    @Column
    private String waivedBy;

    @Column
    private Instant waivedAt;

    @Column(nullable = false)
    private boolean settled = false;

    @Column
    private Instant settledAt;

    public ContributionPenalty(UUID memberId, UUID contributionId, BigDecimal amount, String reason) {
        this.memberId = memberId;
        this.contributionId = contributionId;
        this.amount = amount;
        this.reason = reason;
        this.waived = false;
        this.penaltyDate = LocalDate.now();
    }
}
