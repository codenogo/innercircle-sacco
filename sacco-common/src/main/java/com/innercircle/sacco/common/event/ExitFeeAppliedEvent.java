package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record ExitFeeAppliedEvent(
        UUID exitRequestId,
        UUID memberId,
        BigDecimal amount,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "EXIT_FEE_APPLIED";
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
        return exitRequestId;
    }
}
