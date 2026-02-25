package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record ContributionReceivedEvent(
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

    public ContributionReceivedEvent(
            UUID contributionId,
            UUID memberId,
            BigDecimal amount,
            String referenceNumber,
            UUID correlationId,
            String actor
    ) {
        this(contributionId, memberId, amount, null, amount, BigDecimal.ZERO, referenceNumber, correlationId, actor);
    }

    public ContributionReceivedEvent {
        if (contributionAmount == null) {
            contributionAmount = amount;
        }
        if (welfareAmount == null) {
            welfareAmount = BigDecimal.ZERO;
        }
    }

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
