package com.innercircle.sacco.common.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LoanDisbursedEventTest {

    @Test
    void constructor_shouldSetAllFields() {
        UUID loanId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal interest = new BigDecimal("15000.00");

        LoanDisbursedEvent event = new LoanDisbursedEvent(
                loanId, memberId, principal, interest, "officer"
        );

        assertThat(event.loanId()).isEqualTo(loanId);
        assertThat(event.memberId()).isEqualTo(memberId);
        assertThat(event.principalAmount()).isEqualByComparingTo(principal);
        assertThat(event.interestAmount()).isEqualByComparingTo(interest);
        assertThat(event.actor()).isEqualTo("officer");
    }

    @Test
    void getEventType_shouldReturnLoanDisbursed() {
        LoanDisbursedEvent event = new LoanDisbursedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, BigDecimal.ONE, "actor"
        );

        assertThat(event.getEventType()).isEqualTo("LOAN_DISBURSED");
    }

    @Test
    void getActor_shouldReturnActorValue() {
        LoanDisbursedEvent event = new LoanDisbursedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, BigDecimal.ONE, "loan-officer"
        );

        assertThat(event.getActor()).isEqualTo("loan-officer");
    }

    @Test
    void shouldImplementAuditableEvent() {
        LoanDisbursedEvent event = new LoanDisbursedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, BigDecimal.ONE, "actor"
        );

        assertThat(event).isInstanceOf(AuditableEvent.class);
    }

    @Test
    void equals_shouldBeBasedOnAllFields() {
        UUID lId = UUID.randomUUID();
        UUID mId = UUID.randomUUID();
        BigDecimal p = new BigDecimal("50000");
        BigDecimal i = new BigDecimal("5000");

        LoanDisbursedEvent event1 = new LoanDisbursedEvent(lId, mId, p, i, "actor");
        LoanDisbursedEvent event2 = new LoanDisbursedEvent(lId, mId, p, i, "actor");

        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    void toString_shouldContainFieldValues() {
        LoanDisbursedEvent event = new LoanDisbursedEvent(
                UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("75000"), new BigDecimal("7500"), "admin"
        );

        String str = event.toString();
        assertThat(str).contains("LoanDisbursedEvent");
        assertThat(str).contains("75000");
        assertThat(str).contains("7500");
    }
}
