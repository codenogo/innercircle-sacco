package com.innercircle.sacco.payout.event;

import com.innercircle.sacco.common.event.AuditableEvent;

import java.math.BigDecimal;
import java.util.UUID;

public record ShareWithdrawalProcessedEvent(
        UUID withdrawalId,
        UUID memberId,
        BigDecimal amount,
        BigDecimal newShareBalance,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "SHARE_WITHDRAWAL_PROCESSED";
    }

    @Override
    public String getActor() {
        return actor;
    }
}
