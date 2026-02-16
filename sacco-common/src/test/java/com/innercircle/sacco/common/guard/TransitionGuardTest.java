package com.innercircle.sacco.common.guard;

import com.innercircle.sacco.common.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransitionGuardTest {

    private enum TestStatus { A, B, C }

    private final TransitionGuard<TestStatus> guard = TransitionGuard.<TestStatus>builder("TestEntity")
            .allow(TestStatus.A, TestStatus.B)
            .allow(TestStatus.B, TestStatus.C)
            .build();

    @Test
    void validTransitionDoesNotThrow() {
        assertThatNoException().isThrownBy(() -> guard.validate(TestStatus.A, TestStatus.B));
        assertThatNoException().isThrownBy(() -> guard.validate(TestStatus.B, TestStatus.C));
    }

    @Test
    void invalidTransitionThrowsException() {
        assertThatThrownBy(() -> guard.validate(TestStatus.A, TestStatus.C))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("A")
                .hasMessageContaining("C")
                .hasMessageContaining("TestEntity");
    }

    @Test
    void getAllowedTargetsReturnsCorrectSet() {
        Set<TestStatus> targets = guard.getAllowedTargets(TestStatus.A);

        assertThat(targets).containsExactly(TestStatus.B);
    }

    @Test
    void getAllowedTargetsReturnsEmptySetForTerminalState() {
        Set<TestStatus> targets = guard.getAllowedTargets(TestStatus.C);

        assertThat(targets).isEmpty();
    }

    @Test
    void isAllowedReturnsTrueForValid() {
        assertThat(guard.isAllowed(TestStatus.A, TestStatus.B)).isTrue();
    }

    @Test
    void isAllowedReturnsFalseForInvalid() {
        assertThat(guard.isAllowed(TestStatus.A, TestStatus.C)).isFalse();
        assertThat(guard.isAllowed(TestStatus.C, TestStatus.A)).isFalse();
    }

    @Test
    void exceptionContainsCorrectFields() {
        try {
            guard.validate(TestStatus.A, TestStatus.C);
        } catch (InvalidStateTransitionException e) {
            assertThat(e.getEntityType()).isEqualTo("TestEntity");
            assertThat(e.getCurrentState()).isEqualTo("A");
            assertThat(e.getTargetState()).isEqualTo("C");
            assertThat(e.getMessage()).isEqualTo("Invalid state transition for TestEntity: A -> C");
            return;
        }
        throw new AssertionError("Expected InvalidStateTransitionException");
    }
}
