package com.innercircle.sacco.common.event;

import java.util.UUID;

public record LoanApplicationEvent(
        UUID loanId,
        UUID memberId,
        String action,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "LOAN_APPLICATION";
    }

    @Override
    public String getActor() {
        return actor;
    }
}
