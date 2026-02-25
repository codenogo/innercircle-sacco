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
        UUID categoryId,
        BigDecimal contributionAmount,
        BigDecimal welfareAmount,
        String referenceNumber,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    public ContributionReversedEvent(
            UUID contributionId,
            UUID memberId,
            BigDecimal amount,
            String referenceNumber,
            UUID correlationId,
            String actor
    ) {
        this(contributionId, memberId, amount, null, amount, BigDecimal.ZERO, referenceNumber, correlationId, actor);
    }

    public ContributionReversedEvent {
        if (contributionAmount == null) {
            contributionAmount = amount;
        }
        if (welfareAmount == null) {
            welfareAmount = BigDecimal.ZERO;
        }
    }

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
