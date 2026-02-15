package com.innercircle.sacco.common.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PayoutProcessedEventTest {

    @Test
    void constructor_shouldSetAllFields() {
        UUID payoutId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50000.00");

        PayoutProcessedEvent event = new PayoutProcessedEvent(
                payoutId, memberId, amount, "WITHDRAWAL", "teller"
        );

        assertThat(event.payoutId()).isEqualTo(payoutId);
        assertThat(event.memberId()).isEqualTo(memberId);
        assertThat(event.amount()).isEqualByComparingTo(amount);
        assertThat(event.payoutType()).isEqualTo("WITHDRAWAL");
        assertThat(event.actor()).isEqualTo("teller");
    }

    @Test
    void getEventType_shouldReturnPayoutProcessed() {
        PayoutProcessedEvent event = new PayoutProcessedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "TRANSFER", "actor"
        );

        assertThat(event.getEventType()).isEqualTo("PAYOUT_PROCESSED");
    }

    @Test
    void getActor_shouldReturnActorValue() {
        PayoutProcessedEvent event = new PayoutProcessedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "TRANSFER", "finance-officer"
        );

        assertThat(event.getActor()).isEqualTo("finance-officer");
    }

    @Test
    void shouldImplementAuditableEvent() {
        PayoutProcessedEvent event = new PayoutProcessedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "TRANSFER", "actor"
        );

        assertThat(event).isInstanceOf(AuditableEvent.class);
    }

    @Test
    void equals_shouldBeBasedOnAllFields() {
        UUID pId = UUID.randomUUID();
        UUID mId = UUID.randomUUID();

        PayoutProcessedEvent event1 = new PayoutProcessedEvent(pId, mId, BigDecimal.TEN, "CASH", "actor");
        PayoutProcessedEvent event2 = new PayoutProcessedEvent(pId, mId, BigDecimal.TEN, "CASH", "actor");

        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    void toString_shouldContainFieldValues() {
        PayoutProcessedEvent event = new PayoutProcessedEvent(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("25000"), "BANK_TRANSFER", "actor"
        );

        String str = event.toString();
        assertThat(str).contains("PayoutProcessedEvent");
        assertThat(str).contains("BANK_TRANSFER");
        assertThat(str).contains("25000");
    }
}
