package com.innercircle.sacco.common.event;

import java.util.UUID;

public record MemberCreatedEvent(
        UUID memberId,
        String memberNumber,
        String firstName,
        String lastName,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "MEMBER_CREATED";
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
