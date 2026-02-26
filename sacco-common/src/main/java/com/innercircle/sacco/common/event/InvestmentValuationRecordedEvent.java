package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvestmentValuationRecordedEvent(
        UUID investmentId,
        UUID valuationId,
        BigDecimal marketValue,
        BigDecimal navPerUnit,
        LocalDate valuationDate,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "INVESTMENT_VALUATION_RECORDED";
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
        return valuationId;
    }
}
