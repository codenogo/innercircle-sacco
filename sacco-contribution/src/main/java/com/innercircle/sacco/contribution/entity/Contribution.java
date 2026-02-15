package com.innercircle.sacco.contribution.entity;

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

import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a member contribution to the SACCO/chama.
 * Tracks amount, type, status, and associated metadata.
 */
@Entity
@Table(name = "contributions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contribution extends BaseEntity {

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private ContributionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMode paymentMode;

    @Column(nullable = false)
    private LocalDate contributionMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContributionStatus status;

    @Column(nullable = false)
    private LocalDate contributionDate;

    @Column(unique = true, length = 100)
    private String referenceNumber;

    @Column(length = 500)
    private String notes;

    public Contribution(UUID memberId, BigDecimal amount, ContributionCategory category,
                        PaymentMode paymentMode, LocalDate contributionMonth,
                        LocalDate contributionDate, String referenceNumber, String notes) {
        this.memberId = memberId;
        this.amount = amount;
        this.category = category;
        this.paymentMode = paymentMode;
        this.contributionMonth = contributionMonth;
        this.status = ContributionStatus.PENDING;
        this.contributionDate = contributionDate;
        this.referenceNumber = referenceNumber;
        this.notes = notes;
    }
}
