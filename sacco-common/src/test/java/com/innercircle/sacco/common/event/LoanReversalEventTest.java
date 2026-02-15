package com.innercircle.sacco.common.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LoanReversalEventTest {

    @Test
    void constructor_shouldSetAllFields() {
        UUID reversalId = UUID.randomUUID();
        UUID originalTxId = UUID.randomUUID();
        UUID loanId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("25000.00");
        BigDecimal principal = new BigDecimal("20000.00");
        BigDecimal interest = new BigDecimal("5000.00");

        LoanReversalEvent event = new LoanReversalEvent(
                reversalId, "FULL_REVERSAL", originalTxId, loanId, memberId,
                amount, principal, interest, "Duplicate payment", "admin"
        );

        assertThat(event.reversalId()).isEqualTo(reversalId);
        assertThat(event.reversalType()).isEqualTo("FULL_REVERSAL");
        assertThat(event.originalTransactionId()).isEqualTo(originalTxId);
        assertThat(event.loanId()).isEqualTo(loanId);
        assertThat(event.memberId()).isEqualTo(memberId);
        assertThat(event.amount()).isEqualByComparingTo(amount);
        assertThat(event.principalPortion()).isEqualByComparingTo(principal);
        assertThat(event.interestPortion()).isEqualByComparingTo(interest);
        assertThat(event.reason()).isEqualTo("Duplicate payment");
        assertThat(event.actor()).isEqualTo("admin");
    }

    @Test
    void getEventType_shouldReturnLoanReversal() {
        LoanReversalEvent event = new LoanReversalEvent(
                UUID.randomUUID(), "PARTIAL", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, "reason", "actor"
        );

        assertThat(event.getEventType()).isEqualTo("LOAN_REVERSAL");
    }

    @Test
    void getActor_shouldReturnActorValue() {
        LoanReversalEvent event = new LoanReversalEvent(
                UUID.randomUUID(), "FULL", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, "error", "supervisor"
        );

        assertThat(event.getActor()).isEqualTo("supervisor");
    }

    @Test
    void shouldImplementAuditableEvent() {
        LoanReversalEvent event = new LoanReversalEvent(
                UUID.randomUUID(), "FULL", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, "reason", "actor"
        );

        assertThat(event).isInstanceOf(AuditableEvent.class);
    }

    @Test
    void equals_shouldBeBasedOnAllFields() {
        UUID rId = UUID.randomUUID();
        UUID oId = UUID.randomUUID();
        UUID lId = UUID.randomUUID();
        UUID mId = UUID.randomUUID();

        LoanReversalEvent event1 = new LoanReversalEvent(
                rId, "FULL", oId, lId, mId, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, "r", "a"
        );
        LoanReversalEvent event2 = new LoanReversalEvent(
                rId, "FULL", oId, lId, mId, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, "r", "a"
        );

        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    void toString_shouldContainFieldValues() {
        LoanReversalEvent event = new LoanReversalEvent(
                UUID.randomUUID(), "PARTIAL_REVERSAL", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("3000"), new BigDecimal("2500"), new BigDecimal("500"),
                "Wrong amount", "manager"
        );

        String str = event.toString();
        assertThat(str).contains("LoanReversalEvent");
        assertThat(str).contains("PARTIAL_REVERSAL");
        assertThat(str).contains("Wrong amount");
    }
}
