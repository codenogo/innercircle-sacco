package com.innercircle.sacco.loan.entity;

import com.innercircle.sacco.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "batch_processing_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchProcessingLog extends BaseEntity {

    @Column(name = "processing_month", unique = true, nullable = false, length = 7)
    private String processingMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BatchProcessingStatus status;

    @Column(name = "loans_processed")
    private Integer loansProcessed;

    @Column(name = "interest_accrued", precision = 19, scale = 2)
    private BigDecimal interestAccrued;

    @Column(name = "penalized_loans")
    private Integer penalizedLoans;

    @Column(name = "closed_loans")
    private Integer closedLoans;

    @Column(name = "warnings_summary", columnDefinition = "TEXT")
    private String warningsSummary;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "triggered_by", length = 255)
    private String triggeredBy;
}
