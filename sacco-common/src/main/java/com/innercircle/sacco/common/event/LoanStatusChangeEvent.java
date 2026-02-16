package com.innercircle.sacco.common.event;

import java.util.UUID;

public record LoanStatusChangeEvent(
        UUID loanId,
        String previousStatus,
        String newStatus,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "LOAN_STATUS_CHANGE";
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
