package com.innercircle.sacco.common.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LoanBatchProcessedEventTest {

    @Test
    void constructor_shouldSetAllFields() {
        Instant processedAt = Instant.parse("2026-01-15T10:30:00Z");

        LoanBatchProcessedEvent event = new LoanBatchProcessedEvent(
                50, 5, 3, processedAt, UUID.randomUUID(), "batch-scheduler"
        );

        assertThat(event.processedLoans()).isEqualTo(50);
        assertThat(event.penalizedLoans()).isEqualTo(5);
        assertThat(event.closedLoans()).isEqualTo(3);
        assertThat(event.processedAt()).isEqualTo(processedAt);
        assertThat(event.actor()).isEqualTo("batch-scheduler");
    }

    @Test
    void getEventType_shouldReturnLoanBatchProcessed() {
        LoanBatchProcessedEvent event = new LoanBatchProcessedEvent(
                10, 1, 0, Instant.now(), UUID.randomUUID(), "actor"
        );

        assertThat(event.getEventType()).isEqualTo("LOAN_BATCH_PROCESSED");
    }

    @Test
    void getActor_shouldReturnActorValue() {
        LoanBatchProcessedEvent event = new LoanBatchProcessedEvent(
                10, 1, 0, Instant.now(), UUID.randomUUID(), "cron-job"
        );

        assertThat(event.getActor()).isEqualTo("cron-job");
    }

    @Test
    void shouldImplementAuditableEvent() {
        LoanBatchProcessedEvent event = new LoanBatchProcessedEvent(
                10, 1, 0, Instant.now(), UUID.randomUUID(), "actor"
        );

        assertThat(event).isInstanceOf(AuditableEvent.class);
    }

    @Test
    void equals_shouldBeBasedOnAllFields() {
        Instant now = Instant.now();
        UUID corrId = UUID.randomUUID();

        LoanBatchProcessedEvent event1 = new LoanBatchProcessedEvent(10, 2, 1, now, corrId, "actor");
        LoanBatchProcessedEvent event2 = new LoanBatchProcessedEvent(10, 2, 1, now, corrId, "actor");

        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    void toString_shouldContainFieldValues() {
        LoanBatchProcessedEvent event = new LoanBatchProcessedEvent(
                100, 10, 5, Instant.now(), UUID.randomUUID(), "scheduler"
        );

        String str = event.toString();
        assertThat(str).contains("LoanBatchProcessedEvent");
        assertThat(str).contains("100");
    }

    @Test
    void withZeroCounts_shouldBeAccepted() {
        LoanBatchProcessedEvent event = new LoanBatchProcessedEvent(
                0, 0, 0, Instant.now(), UUID.randomUUID(), "actor"
        );

        assertThat(event.processedLoans()).isEqualTo(0);
        assertThat(event.penalizedLoans()).isEqualTo(0);
        assertThat(event.closedLoans()).isEqualTo(0);
    }
}
