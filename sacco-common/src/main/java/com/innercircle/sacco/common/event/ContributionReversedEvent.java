package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event published when a contribution is reversed.
 * Used for ledger integration and audit trail.
 */
public record ContributionReversedEvent(
        UUID contributionId,
        UUID memberId,
        BigDecimal amount,
        String referenceNumber,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "CONTRIBUTION_REVERSED";
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
