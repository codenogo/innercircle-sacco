package com.innercircle.sacco.loan.guard;

import com.innercircle.sacco.common.exception.InvalidStateTransitionException;
import com.innercircle.sacco.loan.entity.LoanStatus;
import org.junit.jupiter.api.Test;

import static com.innercircle.sacco.loan.entity.LoanStatus.*;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanTransitionGuardsTest {

    @Test
    void pendingToApproved_isAllowed() {
        assertThatNoException().isThrownBy(() -> LoanTransitionGuards.LOAN.validate(PENDING, APPROVED));
    }

    @Test
    void pendingToRejected_isAllowed() {
        assertThatNoException().isThrownBy(() -> LoanTransitionGuards.LOAN.validate(PENDING, REJECTED));
    }

    @Test
    void approvedToDisbursed_isAllowed() {
        assertThatNoException().isThrownBy(() -> LoanTransitionGuards.LOAN.validate(APPROVED, DISBURSED));
    }

    @Test
    void disbursedToRepaying_isAllowed() {
        assertThatNoException().isThrownBy(() -> LoanTransitionGuards.LOAN.validate(DISBURSED, REPAYING));
    }

    @Test
    void repayingToClosed_isAllowed() {
        assertThatNoException().isThrownBy(() -> LoanTransitionGuards.LOAN.validate(REPAYING, CLOSED));
    }

    @Test
    void repayingToDefaulted_isAllowed() {
        assertThatNoException().isThrownBy(() -> LoanTransitionGuards.LOAN.validate(REPAYING, DEFAULTED));
    }

    @Test
    void pendingToDisbursed_isNotAllowed() {
        assertThatThrownBy(() -> LoanTransitionGuards.LOAN.validate(PENDING, DISBURSED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void closedToPending_isNotAllowed() {
        assertThatThrownBy(() -> LoanTransitionGuards.LOAN.validate(CLOSED, PENDING))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
