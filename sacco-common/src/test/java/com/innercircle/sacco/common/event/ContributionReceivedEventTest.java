package com.innercircle.sacco.common.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContributionReceivedEventTest {

    @Test
    void constructor_shouldSetAllFields() {
        UUID contributionId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("5000.00");

        ContributionReceivedEvent event = new ContributionReceivedEvent(
                contributionId, memberId, amount, "REF-001", "admin"
        );

        assertThat(event.contributionId()).isEqualTo(contributionId);
        assertThat(event.memberId()).isEqualTo(memberId);
        assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(event.referenceNumber()).isEqualTo("REF-001");
        assertThat(event.actor()).isEqualTo("admin");
    }

    @Test
    void getEventType_shouldReturnContributionReceived() {
        ContributionReceivedEvent event = new ContributionReceivedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "REF", "user"
        );

        assertThat(event.getEventType()).isEqualTo("CONTRIBUTION_RECEIVED");
    }

    @Test
    void getActor_shouldReturnActorValue() {
        ContributionReceivedEvent event = new ContributionReceivedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ONE, "REF", "treasurer"
        );

        assertThat(event.getActor()).isEqualTo("treasurer");
    }

    @Test
    void shouldImplementAuditableEvent() {
        ContributionReceivedEvent event = new ContributionReceivedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ONE, "REF", "user"
        );

        assertThat(event).isInstanceOf(AuditableEvent.class);
    }

    @Test
    void equals_shouldBeBasedOnAllFields() {
        UUID cId = UUID.randomUUID();
        UUID mId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.TEN;

        ContributionReceivedEvent event1 = new ContributionReceivedEvent(cId, mId, amount, "REF", "actor");
        ContributionReceivedEvent event2 = new ContributionReceivedEvent(cId, mId, amount, "REF", "actor");

        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    void notEquals_shouldDetectDifferences() {
        ContributionReceivedEvent event1 = new ContributionReceivedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ONE, "REF-1", "user1"
        );
        ContributionReceivedEvent event2 = new ContributionReceivedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "REF-2", "user2"
        );

        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    void toString_shouldContainFieldValues() {
        ContributionReceivedEvent event = new ContributionReceivedEvent(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100"), "REF-999", "actor"
        );

        String str = event.toString();
        assertThat(str).contains("ContributionReceivedEvent");
        assertThat(str).contains("REF-999");
        assertThat(str).contains("100");
    }
}
