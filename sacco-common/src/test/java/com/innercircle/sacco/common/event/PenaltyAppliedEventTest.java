package com.innercircle.sacco.common.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PenaltyAppliedEventTest {

    @Test
    void constructor_shouldSetAllFields() {
        UUID penaltyId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("500.00");

        PenaltyAppliedEvent event = new PenaltyAppliedEvent(
                penaltyId, memberId, amount, "LATE_PAYMENT", UUID.randomUUID(), "system"
        );

        assertThat(event.penaltyId()).isEqualTo(penaltyId);
        assertThat(event.memberId()).isEqualTo(memberId);
        assertThat(event.amount()).isEqualByComparingTo(amount);
        assertThat(event.penaltyType()).isEqualTo("LATE_PAYMENT");
        assertThat(event.actor()).isEqualTo("system");
    }

    @Test
    void getEventType_shouldReturnPenaltyApplied() {
        PenaltyAppliedEvent event = new PenaltyAppliedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ONE, "OVERDUE", UUID.randomUUID(), "actor"
        );

        assertThat(event.getEventType()).isEqualTo("PENALTY_APPLIED");
    }

    @Test
    void getActor_shouldReturnActorValue() {
        PenaltyAppliedEvent event = new PenaltyAppliedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ONE, "OVERDUE", UUID.randomUUID(), "batch-processor"
        );

        assertThat(event.getActor()).isEqualTo("batch-processor");
    }

    @Test
    void shouldImplementAuditableEvent() {
        PenaltyAppliedEvent event = new PenaltyAppliedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ONE, "OVERDUE", UUID.randomUUID(), "actor"
        );

        assertThat(event).isInstanceOf(AuditableEvent.class);
    }

    @Test
    void equals_shouldBeBasedOnAllFields() {
        UUID pId = UUID.randomUUID();
        UUID mId = UUID.randomUUID();
        UUID cId = UUID.randomUUID();

        PenaltyAppliedEvent event1 = new PenaltyAppliedEvent(pId, mId, BigDecimal.TEN, "LATE", cId, "actor");
        PenaltyAppliedEvent event2 = new PenaltyAppliedEvent(pId, mId, BigDecimal.TEN, "LATE", cId, "actor");

        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    void toString_shouldContainFieldValues() {
        PenaltyAppliedEvent event = new PenaltyAppliedEvent(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("750"), "MISSED_PAYMENT", UUID.randomUUID(), "actor"
        );

        String str = event.toString();
        assertThat(str).contains("PenaltyAppliedEvent");
        assertThat(str).contains("MISSED_PAYMENT");
        assertThat(str).contains("750");
    }

    @Test
    void notEquals_withDifferentPenaltyType_shouldNotBeEqual() {
        UUID pId = UUID.randomUUID();
        UUID mId = UUID.randomUUID();
        UUID cId = UUID.randomUUID();

        PenaltyAppliedEvent event1 = new PenaltyAppliedEvent(pId, mId, BigDecimal.TEN, "LATE", cId, "actor");
        PenaltyAppliedEvent event2 = new PenaltyAppliedEvent(pId, mId, BigDecimal.TEN, "OVERDUE", cId, "actor");

        assertThat(event1).isNotEqualTo(event2);
    }
}
