package com.innercircle.sacco.common.event;

import java.util.UUID;

public record InvestmentRejectedEvent(
        UUID investmentId,
        String referenceNumber,
        String reason,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "INVESTMENT_REJECTED";
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
        return investmentId;
    }
}
