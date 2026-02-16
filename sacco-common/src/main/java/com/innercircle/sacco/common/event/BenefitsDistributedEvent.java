package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record BenefitsDistributedEvent(
        UUID loanId,
        BigDecimal totalInterestAmount,
        int beneficiaryCount,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "BENEFITS_DISTRIBUTED";
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
