package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record ContributionCreatedEvent(
        UUID contributionId,
        UUID memberId,
        BigDecimal amount,
        String referenceNumber,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "CONTRIBUTION_CREATED";
    }

    @Override
    public String getActor() {
        return actor;
    }
}
