package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record LoanReversalEvent(
        UUID reversalId,
        String reversalType,
        UUID originalTransactionId,
        UUID loanId,
        UUID memberId,
        BigDecimal amount,
        BigDecimal principalPortion,
        BigDecimal interestPortion,
        String reason,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "LOAN_REVERSAL";
    }

    @Override
    public String getActor() {
        return actor;
    }
}
