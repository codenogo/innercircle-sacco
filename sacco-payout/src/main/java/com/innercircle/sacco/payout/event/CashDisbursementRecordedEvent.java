package com.innercircle.sacco.payout.event;

import com.innercircle.sacco.common.event.AuditableEvent;

import java.math.BigDecimal;
import java.util.UUID;

public record CashDisbursementRecordedEvent(
        UUID disbursementId,
        UUID memberId,
        BigDecimal amount,
        String receiptNumber,
        String actor
) implements AuditableEvent {

    @Override
    public String getEventType() {
        return "CASH_DISBURSEMENT_RECORDED";
    }

    @Override
    public String getActor() {
        return actor;
    }
}
