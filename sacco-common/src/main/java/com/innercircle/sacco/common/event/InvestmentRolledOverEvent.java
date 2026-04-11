package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvestmentRolledOverEvent(
        UUID investmentId,
        String referenceNumber,
        LocalDate previousMaturityDate,
        LocalDate newMaturityDate,
        BigDecimal newInterestRate,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "INVESTMENT_ROLLED_OVER";
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
        return investmentId;
    }
}
