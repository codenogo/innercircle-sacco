package com.innercircle.sacco.payout.guard;

import com.innercircle.sacco.common.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import static com.innercircle.sacco.payout.entity.PayoutStatus.*;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayoutTransitionGuardsTest {

    @Test
    void pendingToApproved_isAllowed() {
        assertThatNoException().isThrownBy(() -> PayoutTransitionGuards.PAYOUT.validate(PENDING, APPROVED));
    }

    @Test
    void approvedToProcessed_isAllowed() {
        assertThatNoException().isThrownBy(() -> PayoutTransitionGuards.PAYOUT.validate(APPROVED, PROCESSED));
    }

    @Test
    void approvedToFailed_isAllowed() {
        assertThatNoException().isThrownBy(() -> PayoutTransitionGuards.PAYOUT.validate(APPROVED, FAILED));
    }

    @Test
    void pendingToFailed_isAllowed() {
        assertThatNoException().isThrownBy(() -> PayoutTransitionGuards.PAYOUT.validate(PENDING, FAILED));
    }

    @Test
    void processedToPending_isNotAllowed() {
        assertThatThrownBy(() -> PayoutTransitionGuards.PAYOUT.validate(PROCESSED, PENDING))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void failedToApproved_isNotAllowed() {
        assertThatThrownBy(() -> PayoutTransitionGuards.PAYOUT.validate(FAILED, APPROVED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
