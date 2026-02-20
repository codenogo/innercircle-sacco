package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PayoutProcessedEvent(
        UUID payoutId,
        UUID memberId,
        BigDecimal amount,
        String payoutType,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "PAYOUT_PROCESSED";
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
        return payoutId;
    }
}
