package com.innercircle.sacco.contribution.guard;

import com.innercircle.sacco.common.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import static com.innercircle.sacco.contribution.entity.ContributionStatus.*;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContributionTransitionGuardsTest {

    @Test
    void pendingToConfirmed_isAllowed() {
        assertThatNoException().isThrownBy(
                () -> ContributionTransitionGuards.CONTRIBUTION.validate(PENDING, CONFIRMED));
    }

    @Test
    void confirmedToReversed_isAllowed() {
        assertThatNoException().isThrownBy(
                () -> ContributionTransitionGuards.CONTRIBUTION.validate(CONFIRMED, REVERSED));
    }

    @Test
    void pendingToReversed_isNotAllowed() {
        assertThatThrownBy(() -> ContributionTransitionGuards.CONTRIBUTION.validate(PENDING, REVERSED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void reversedToPending_isNotAllowed() {
        assertThatThrownBy(() -> ContributionTransitionGuards.CONTRIBUTION.validate(REVERSED, PENDING))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
