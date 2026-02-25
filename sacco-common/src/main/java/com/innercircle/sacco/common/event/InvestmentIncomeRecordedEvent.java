package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record InvestmentIncomeRecordedEvent(
        UUID investmentId,
        UUID incomeId,
        String incomeType,
        BigDecimal amount,
        String referenceNumber,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "INVESTMENT_INCOME_RECORDED";
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
        return incomeId;
    }
}
