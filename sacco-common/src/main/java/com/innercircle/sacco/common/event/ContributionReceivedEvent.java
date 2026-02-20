package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record ContributionReceivedEvent(
        UUID contributionId,
        UUID memberId,
        BigDecimal amount,
        String referenceNumber,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "CONTRIBUTION_RECEIVED";
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
        return contributionId;
    }
}
