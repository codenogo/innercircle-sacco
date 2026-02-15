package com.innercircle.sacco.common.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BenefitsDistributedEventTest {

    @Test
    void constructor_shouldSetAllFields() {
        UUID loanId = UUID.randomUUID();
        BigDecimal totalInterest = new BigDecimal("15000.00");

        BenefitsDistributedEvent event = new BenefitsDistributedEvent(
                loanId, totalInterest, 5, "system"
        );

        assertThat(event.loanId()).isEqualTo(loanId);
        assertThat(event.totalInterestAmount()).isEqualByComparingTo(totalInterest);
        assertThat(event.beneficiaryCount()).isEqualTo(5);
        assertThat(event.actor()).isEqualTo("system");
    }

    @Test
    void getEventType_shouldReturnBenefitsDistributed() {
        BenefitsDistributedEvent event = new BenefitsDistributedEvent(
                UUID.randomUUID(), BigDecimal.TEN, 3, "actor"
        );

        assertThat(event.getEventType()).isEqualTo("BENEFITS_DISTRIBUTED");
    }

    @Test
    void getActor_shouldReturnActorValue() {
        BenefitsDistributedEvent event = new BenefitsDistributedEvent(
                UUID.randomUUID(), BigDecimal.TEN, 1, "scheduler"
        );

        assertThat(event.getActor()).isEqualTo("scheduler");
    }

    @Test
    void shouldImplementAuditableEvent() {
        BenefitsDistributedEvent event = new BenefitsDistributedEvent(
                UUID.randomUUID(), BigDecimal.TEN, 1, "actor"
        );

        assertThat(event).isInstanceOf(AuditableEvent.class);
    }

    @Test
    void equals_shouldBeBasedOnAllFields() {
        UUID lId = UUID.randomUUID();

        BenefitsDistributedEvent event1 = new BenefitsDistributedEvent(lId, BigDecimal.TEN, 3, "actor");
        BenefitsDistributedEvent event2 = new BenefitsDistributedEvent(lId, BigDecimal.TEN, 3, "actor");

        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    void toString_shouldContainFieldValues() {
        BenefitsDistributedEvent event = new BenefitsDistributedEvent(
                UUID.randomUUID(), new BigDecimal("12000"), 7, "admin"
        );

        String str = event.toString();
        assertThat(str).contains("BenefitsDistributedEvent");
        assertThat(str).contains("12000");
    }

    @Test
    void beneficiaryCount_withZero_shouldBeAccepted() {
        BenefitsDistributedEvent event = new BenefitsDistributedEvent(
                UUID.randomUUID(), BigDecimal.ZERO, 0, "actor"
        );

        assertThat(event.beneficiaryCount()).isEqualTo(0);
    }
}
