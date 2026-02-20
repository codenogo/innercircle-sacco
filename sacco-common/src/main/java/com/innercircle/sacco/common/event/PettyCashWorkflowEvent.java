package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PettyCashWorkflowEvent(
        UUID voucherId,
        String action,
        BigDecimal amount,
        String expenseType,
        String referenceNumber,
        String receiptNumber,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "PETTY_CASH_" + action;
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
        return voucherId;
    }
}
