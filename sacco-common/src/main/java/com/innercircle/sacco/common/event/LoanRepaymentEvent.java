package com.innercircle.sacco.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record LoanRepaymentEvent(
        UUID loanId,
        UUID memberId,
        UUID repaymentId,
        BigDecimal amount,
        BigDecimal principalPortion,
        BigDecimal interestPortion,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "LOAN_REPAYMENT";
    }

    @Override
    public String getActor() {
        return actor;
    }
}
