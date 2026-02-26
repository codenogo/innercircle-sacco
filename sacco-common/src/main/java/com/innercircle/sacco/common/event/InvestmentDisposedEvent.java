package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record InvestmentDisposedEvent(
        UUID investmentId,
        String referenceNumber,
        String disposalType,
        BigDecimal proceedsAmount,
        BigDecimal fees,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "INVESTMENT_DISPOSED";
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
