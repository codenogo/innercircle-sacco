package com.innercircle.sacco.common.event;

import java.util.UUID;

public record LoanApplicationEvent(
        UUID loanId,
        UUID memberId,
        String action,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "LOAN_APPLICATION";
    }

    @Override
    public String getActor() {
        return actor;
    }

    @Override
    public UUID getCorrelationId() {
        return correlationId;
    }

    @Override
    public UUID getEntityId() {
        return loanId;
    }
}
