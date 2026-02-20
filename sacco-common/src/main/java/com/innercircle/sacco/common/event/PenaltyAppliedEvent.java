package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PenaltyAppliedEvent(
        UUID penaltyId,
        UUID memberId,
        BigDecimal amount,
        String penaltyType,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "PENALTY_APPLIED";
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
        return penaltyId;
    }
}
