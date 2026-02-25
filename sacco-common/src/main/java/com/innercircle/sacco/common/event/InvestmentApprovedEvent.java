package com.innercircle.sacco.common.event;

import java.util.UUID;

public record InvestmentApprovedEvent(
        UUID investmentId,
        String referenceNumber,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "INVESTMENT_APPROVED";
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
