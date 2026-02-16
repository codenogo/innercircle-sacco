package com.innercircle.sacco.member.guard;

import com.innercircle.sacco.common.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import static com.innercircle.sacco.member.entity.MemberStatus.*;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTransitionGuardsTest {

    @Test
    void activeToSuspended_isAllowed() {
        assertThatNoException().isThrownBy(() -> MemberTransitionGuards.MEMBER.validate(ACTIVE, SUSPENDED));
    }

    @Test
    void suspendedToActive_isAllowed() {
        assertThatNoException().isThrownBy(() -> MemberTransitionGuards.MEMBER.validate(SUSPENDED, ACTIVE));
    }

    @Test
    void activeToDeactivated_isAllowed() {
        assertThatNoException().isThrownBy(() -> MemberTransitionGuards.MEMBER.validate(ACTIVE, DEACTIVATED));
    }

    @Test
    void suspendedToDeactivated_isAllowed() {
        assertThatNoException().isThrownBy(
                () -> MemberTransitionGuards.MEMBER.validate(SUSPENDED, DEACTIVATED));
    }

    @Test
    void deactivatedToActive_isNotAllowed() {
        assertThatThrownBy(() -> MemberTransitionGuards.MEMBER.validate(DEACTIVATED, ACTIVE))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void deactivatedToSuspended_isNotAllowed() {
        assertThatThrownBy(() -> MemberTransitionGuards.MEMBER.validate(DEACTIVATED, SUSPENDED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
