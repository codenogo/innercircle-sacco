package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record LoanDisbursedEvent(
        UUID loanId,
        UUID memberId,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "LOAN_DISBURSED";
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
        return loanId;
    }
}
