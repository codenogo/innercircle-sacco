package com.innercircle.sacco.common.event;

import java.util.UUID;

public record PayoutStatusChangeEvent(
        UUID payoutId,
        UUID memberId,
        String action,
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
}
