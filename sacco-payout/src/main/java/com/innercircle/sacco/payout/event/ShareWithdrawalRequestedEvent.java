package com.innercircle.sacco.payout.event;

import com.innercircle.sacco.common.event.AuditableEvent;

import java.math.BigDecimal;
import java.util.UUID;

public record ShareWithdrawalRequestedEvent(
        UUID withdrawalId,
        UUID memberId,
        BigDecimal amount,
        String withdrawalType,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "SHARE_WITHDRAWAL_REQUESTED";
    }

    @Override
    public String getActor() {
        return actor;
    }
}
