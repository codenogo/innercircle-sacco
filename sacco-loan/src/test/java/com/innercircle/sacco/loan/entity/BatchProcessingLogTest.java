package com.innercircle.sacco.loan.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BatchProcessingLogTest {

    @Test
    @DisplayName("builder should create entity with all fields set correctly")
    void builderShouldCreateEntityWithAllFields() {
        String processingDate = "2026-02-15";
        BatchProcessingStatus status = BatchProcessingStatus.COMPLETED;
        Integer loansProcessed = 150;
        BigDecimal interestAccrued = new BigDecimal("5000.50");
        Integer penalizedLoans = 10;
        Integer closedLoans = 5;
        String warningsSummary = "3 loans overdue";
        Instant startedAt = Instant.parse("2026-02-15T08:00:00Z");
        Instant completedAt = Instant.parse("2026-02-15T08:30:00Z");
        String triggeredBy = "admin@sacco.com";

        BatchProcessingLog log = BatchProcessingLog.builder()
                .processingDate(processingDate)
                .status(status)
                .loansProcessed(loansProcessed)
                .interestAccrued(interestAccrued)
                .penalizedLoans(penalizedLoans)
                .closedLoans(closedLoans)
                .warningsSummary(warningsSummary)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .triggeredBy(triggeredBy)
                .build();

        assertThat(log).isNotNull();
        assertThat(log.getProcessingDate()).isEqualTo(processingDate);
        assertThat(log.getStatus()).isEqualTo(status);
        assertThat(log.getLoansProcessed()).isEqualTo(loansProcessed);
        assertThat(log.getInterestAccrued()).isEqualTo(interestAccrued);
        assertThat(log.getPenalizedLoans()).isEqualTo(penalizedLoans);
        assertThat(log.getClosedLoans()).isEqualTo(closedLoans);
        assertThat(log.getWarningsSummary()).isEqualTo(warningsSummary);
        assertThat(log.getStartedAt()).isEqualTo(startedAt);
        assertThat(log.getCompletedAt()).isEqualTo(completedAt);
        assertThat(log.getTriggeredBy()).isEqualTo(triggeredBy);
    }

    @Test
    @DisplayName("builder should create entity with partial fields")
    void builderShouldCreateEntityWithPartialFields() {
        String processingDate = "2026-02-15";
        BatchProcessingStatus status = BatchProcessingStatus.STARTED;
        Instant startedAt = Instant.now();

        BatchProcessingLog log = BatchProcessingLog.builder()
                .processingDate(processingDate)
                .status(status)
                .startedAt(startedAt)
                .build();

        assertThat(log).isNotNull();
        assertThat(log.getProcessingDate()).isEqualTo(processingDate);
        assertThat(log.getStatus()).isEqualTo(status);
        assertThat(log.getStartedAt()).isEqualTo(startedAt);
        assertThat(log.getLoansProcessed()).isNull();
        assertThat(log.getInterestAccrued()).isNull();
        assertThat(log.getPenalizedLoans()).isNull();
        assertThat(log.getClosedLoans()).isNull();
        assertThat(log.getWarningsSummary()).isNull();
        assertThat(log.getCompletedAt()).isNull();
        assertThat(log.getTriggeredBy()).isNull();
    }

    @Test
    @DisplayName("default constructor should create empty entity")
    void defaultConstructorShouldWork() {
        BatchProcessingLog log = new BatchProcessingLog();

        assertThat(log).isNotNull();
        assertThat(log.getProcessingDate()).isNull();
        assertThat(log.getStatus()).isNull();
        assertThat(log.getLoansProcessed()).isNull();
        assertThat(log.getInterestAccrued()).isNull();
        assertThat(log.getPenalizedLoans()).isNull();
        assertThat(log.getClosedLoans()).isNull();
        assertThat(log.getWarningsSummary()).isNull();
        assertThat(log.getStartedAt()).isNull();
        assertThat(log.getCompletedAt()).isNull();
        assertThat(log.getTriggeredBy()).isNull();
    }

    @Test
    @DisplayName("setters should set fields correctly")
    void settersShouldWork() {
        BatchProcessingLog log = new BatchProcessingLog();

        String processingDate = "2026-03-01";
        BatchProcessingStatus status = BatchProcessingStatus.FAILED;
        Integer loansProcessed = 75;
        BigDecimal interestAccrued = new BigDecimal("2500.00");
        Integer penalizedLoans = 8;
        Integer closedLoans = 2;
        String warningsSummary = "Database connection error";
        Instant startedAt = Instant.parse("2026-03-01T10:00:00Z");
        Instant completedAt = Instant.parse("2026-03-01T10:15:00Z");
        String triggeredBy = "scheduler";

        log.setProcessingDate(processingDate);
        log.setStatus(status);
        log.setLoansProcessed(loansProcessed);
        log.setInterestAccrued(interestAccrued);
        log.setPenalizedLoans(penalizedLoans);
        log.setClosedLoans(closedLoans);
        log.setWarningsSummary(warningsSummary);
        log.setStartedAt(startedAt);
        log.setCompletedAt(completedAt);
        log.setTriggeredBy(triggeredBy);

        assertThat(log.getProcessingDate()).isEqualTo(processingDate);
        assertThat(log.getStatus()).isEqualTo(status);
        assertThat(log.getLoansProcessed()).isEqualTo(loansProcessed);
        assertThat(log.getInterestAccrued()).isEqualTo(interestAccrued);
        assertThat(log.getPenalizedLoans()).isEqualTo(penalizedLoans);
        assertThat(log.getClosedLoans()).isEqualTo(closedLoans);
        assertThat(log.getWarningsSummary()).isEqualTo(warningsSummary);
        assertThat(log.getStartedAt()).isEqualTo(startedAt);
        assertThat(log.getCompletedAt()).isEqualTo(completedAt);
        assertThat(log.getTriggeredBy()).isEqualTo(triggeredBy);
    }

    @Test
    @DisplayName("getters should retrieve set values correctly")
    void gettersShouldWork() {
        Instant now = Instant.now();
        BatchProcessingLog log = BatchProcessingLog.builder()
                .processingDate("2026-01-15")
                .status(BatchProcessingStatus.STARTED)
                .loansProcessed(100)
                .interestAccrued(BigDecimal.TEN)
                .penalizedLoans(5)
                .closedLoans(3)
                .warningsSummary("All good")
                .startedAt(now)
                .completedAt(now)
                .triggeredBy("user")
                .build();

        assertThat(log.getProcessingDate()).isEqualTo("2026-01-15");
        assertThat(log.getStatus()).isEqualTo(BatchProcessingStatus.STARTED);
        assertThat(log.getLoansProcessed()).isEqualTo(100);
        assertThat(log.getInterestAccrued()).isEqualTo(BigDecimal.TEN);
        assertThat(log.getPenalizedLoans()).isEqualTo(5);
        assertThat(log.getClosedLoans()).isEqualTo(3);
        assertThat(log.getWarningsSummary()).isEqualTo("All good");
        assertThat(log.getStartedAt()).isEqualTo(now);
        assertThat(log.getCompletedAt()).isEqualTo(now);
        assertThat(log.getTriggeredBy()).isEqualTo("user");
    }

    @Test
    @DisplayName("should extend BaseEntity")
    void shouldExtendBaseEntity() {
        BatchProcessingLog log = new BatchProcessingLog();

        assertThat(log).isInstanceOf(com.innercircle.sacco.common.model.BaseEntity.class);
    }

    @Test
    @DisplayName("builder should handle null values")
    void builderShouldHandleNullValues() {
        BatchProcessingLog log = BatchProcessingLog.builder()
                .processingDate("2026-02-15")
                .status(BatchProcessingStatus.COMPLETED)
                .startedAt(Instant.now())
                .loansProcessed(null)
                .interestAccrued(null)
                .penalizedLoans(null)
                .closedLoans(null)
                .warningsSummary(null)
                .completedAt(null)
                .triggeredBy(null)
                .build();

        assertThat(log.getProcessingDate()).isEqualTo("2026-02-15");
        assertThat(log.getStatus()).isEqualTo(BatchProcessingStatus.COMPLETED);
        assertThat(log.getLoansProcessed()).isNull();
        assertThat(log.getInterestAccrued()).isNull();
        assertThat(log.getPenalizedLoans()).isNull();
        assertThat(log.getClosedLoans()).isNull();
        assertThat(log.getWarningsSummary()).isNull();
        assertThat(log.getCompletedAt()).isNull();
        assertThat(log.getTriggeredBy()).isNull();
    }
}
