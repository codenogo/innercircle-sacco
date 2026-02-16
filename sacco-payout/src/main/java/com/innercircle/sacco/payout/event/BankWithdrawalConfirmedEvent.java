package com.innercircle.sacco.payout.event;

import com.innercircle.sacco.common.event.AuditableEvent;

import java.util.UUID;

public record BankWithdrawalConfirmedEvent(
        UUID withdrawalId,
        UUID memberId,
        String referenceNumber,
        UUID correlationId,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "BANK_WITHDRAWAL_CONFIRMED";
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
