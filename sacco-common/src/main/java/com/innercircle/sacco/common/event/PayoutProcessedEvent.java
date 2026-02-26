package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PayoutProcessedEvent(
        UUID payoutId,
        UUID memberId,
        BigDecimal amount,
        String payoutType,
        String sourceType,
        UUID sourceId,
        Integer installmentNumber,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    public PayoutProcessedEvent(
            UUID payoutId,
            UUID memberId,
            BigDecimal amount,
            String payoutType,
            UUID correlationId,
            String actor
    ) {
        this(payoutId, memberId, amount, payoutType, null, null, null, correlationId, actor);
    }

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
