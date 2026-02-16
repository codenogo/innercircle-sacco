package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PenaltyWaivedEvent(
        UUID penaltyId,
        UUID memberId,
        BigDecimal amount,
        String reason,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "PENALTY_WAIVED";
    }

    @Override
    public String getActor() {
        return actor;
    }

    @Override
    public UUID getCorrelationId() {
        return correlationId;
    }
}
