package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PenaltyPaidEvent(
        UUID penaltyId,
        UUID memberId,
        BigDecimal amount,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "PENALTY_PAID";
    }

    @Override
    public String getActor() {
        return actor;
    }
}
