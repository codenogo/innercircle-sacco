package com.innercircle.sacco.payout.event;

import com.innercircle.sacco.common.event.AuditableEvent;

import java.math.BigDecimal;
import java.util.UUID;

public record BankWithdrawalInitiatedEvent(
        UUID withdrawalId,
        UUID memberId,
        BigDecimal amount,
        String bankName,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "BANK_WITHDRAWAL_INITIATED";
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
