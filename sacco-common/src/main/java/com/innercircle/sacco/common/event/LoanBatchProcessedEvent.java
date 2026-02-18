package com.innercircle.sacco.common.event;

import java.time.Instant;
import java.util.UUID;

public record LoanBatchProcessedEvent(
        Integer processedLoans,
        Integer penalizedLoans,
        Integer closedLoans,
        Instant processedAt,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "LOAN_BATCH_PROCESSED";
    }

    @Override
    public String getActor() {
        return actor;
    }

    @Override
    public UUID getCorrelationId() {
        return correlationId;
    }
}
