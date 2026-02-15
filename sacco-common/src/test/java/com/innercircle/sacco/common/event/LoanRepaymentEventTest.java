package com.innercircle.sacco.common.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LoanRepaymentEventTest {

    @Test
    void constructor_shouldSetAllFields() {
        UUID loanId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID repaymentId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10000.00");
        BigDecimal principal = new BigDecimal("8000.00");
        BigDecimal interest = new BigDecimal("2000.00");

        LoanRepaymentEvent event = new LoanRepaymentEvent(
                loanId, memberId, repaymentId, amount, principal, interest, "member"
        );

        assertThat(event.loanId()).isEqualTo(loanId);
        assertThat(event.memberId()).isEqualTo(memberId);
        assertThat(event.repaymentId()).isEqualTo(repaymentId);
        assertThat(event.amount()).isEqualByComparingTo(amount);
        assertThat(event.principalPortion()).isEqualByComparingTo(principal);
        assertThat(event.interestPortion()).isEqualByComparingTo(interest);
        assertThat(event.actor()).isEqualTo("member");
    }

    @Test
    void getEventType_shouldReturnLoanRepayment() {
        LoanRepaymentEvent event = new LoanRepaymentEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, "actor"
        );

        assertThat(event.getEventType()).isEqualTo("LOAN_REPAYMENT");
    }

    @Test
    void getActor_shouldReturnActorValue() {
        LoanRepaymentEvent event = new LoanRepaymentEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, "cashier"
        );

        assertThat(event.getActor()).isEqualTo("cashier");
    }

    @Test
    void shouldImplementAuditableEvent() {
        LoanRepaymentEvent event = new LoanRepaymentEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, "actor"
        );

        assertThat(event).isInstanceOf(AuditableEvent.class);
    }

    @Test
    void equals_shouldBeBasedOnAllFields() {
        UUID lId = UUID.randomUUID();
        UUID mId = UUID.randomUUID();
        UUID rId = UUID.randomUUID();

        LoanRepaymentEvent event1 = new LoanRepaymentEvent(lId, mId, rId, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, "a");
        LoanRepaymentEvent event2 = new LoanRepaymentEvent(lId, mId, rId, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, "a");

        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    void toString_shouldContainFieldValues() {
        LoanRepaymentEvent event = new LoanRepaymentEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("5000"), new BigDecimal("4000"), new BigDecimal("1000"), "actor"
        );

        String str = event.toString();
        assertThat(str).contains("LoanRepaymentEvent");
        assertThat(str).contains("5000");
    }
}
