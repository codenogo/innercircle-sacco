package com.innercircle.sacco.payout.event;

import com.innercircle.sacco.common.event.AuditableEvent;

import java.math.BigDecimal;
import java.util.UUID;

public record BankWithdrawalApprovedEvent(
        UUID withdrawalId,
        UUID memberId,
        BigDecimal amount,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "BANK_WITHDRAWAL_APPROVED";
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
