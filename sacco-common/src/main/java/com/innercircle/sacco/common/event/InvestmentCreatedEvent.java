package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record InvestmentCreatedEvent(
        UUID investmentId,
        String referenceNumber,
        String investmentType,
        BigDecimal purchasePrice,
        BigDecimal currentValue,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "INVESTMENT_CREATED";
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
