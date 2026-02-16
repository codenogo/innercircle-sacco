package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LoanInterestAccrualEvent(
        UUID loanId,
        UUID memberId,
        BigDecimal interestAmount,
        BigDecimal outstandingBalance,
        LocalDate accrualDate,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "INTEREST_ACCRUAL";
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
