package com.innercircle.sacco.common.event;

import java.util.UUID;

public record MemberStatusChangeEvent(
        UUID memberId,
        String memberNumber,
        String previousStatus,
        String newStatus,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "MEMBER_STATUS_CHANGE";
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
