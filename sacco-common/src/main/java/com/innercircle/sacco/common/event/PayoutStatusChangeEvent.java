package com.innercircle.sacco.common.event;

import java.util.UUID;

public record PayoutStatusChangeEvent(
        UUID payoutId,
        UUID memberId,
        String action,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "PAYOUT_STATUS_CHANGE";
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
