package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record InvestmentActivatedEvent(
        UUID investmentId,
        String referenceNumber,
        BigDecimal amount,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "INVESTMENT_ACTIVATED";
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
