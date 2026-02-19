package com.innercircle.sacco.common.guard;

import com.innercircle.sacco.common.exception.MakerCheckerViolationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MakerCheckerGuardTest {

    private static final String ENTITY_TYPE = "LoanApplication";
    private static final UUID ENTITY_ID = UUID.randomUUID();

    // --- assertDifferentActor ---

    @Test
    void assertDifferentActor_differentActors_doesNotThrow() {
        assertThatNoException().isThrownBy(() ->
                MakerCheckerGuard.assertDifferentActor("alice", "bob", ENTITY_TYPE, ENTITY_ID));
    }

    @Test
    void assertDifferentActor_sameActor_throwsViolation() {
        assertThatThrownBy(() ->
                MakerCheckerGuard.assertDifferentActor("alice", "alice", ENTITY_TYPE, ENTITY_ID))
                .isInstanceOf(MakerCheckerViolationException.class)
                .hasMessageContaining("Maker-checker violation")
                .hasMessageContaining(ENTITY_TYPE);
    }

    @Test
    void assertDifferentActor_nullMaker_doesNotThrow() {
        assertThatNoException().isThrownBy(() ->
                MakerCheckerGuard.assertDifferentActor(null, "bob", ENTITY_TYPE, ENTITY_ID));
    }

    @Test
    void assertDifferentActor_nullChecker_doesNotThrow() {
        assertThatNoException().isThrownBy(() ->
                MakerCheckerGuard.assertDifferentActor("alice", null, ENTITY_TYPE, ENTITY_ID));
    }

    // --- assertOrOverride ---

    @Test
    void assertOrOverride_differentActors_returnsFalse() {
        boolean override = MakerCheckerGuard.assertOrOverride(
                "alice", "bob", null, false, ENTITY_TYPE, ENTITY_ID);
        assertThat(override).isFalse();
    }

    @Test
    void assertOrOverride_sameActorNotAdmin_throwsViolation() {
        assertThatThrownBy(() ->
                MakerCheckerGuard.assertOrOverride(
                        "alice", "alice", "reason", false, ENTITY_TYPE, ENTITY_ID))
                .isInstanceOf(MakerCheckerViolationException.class);
    }

    @Test
    void assertOrOverride_sameActorAdminWithReason_returnsTrue() {
        boolean override = MakerCheckerGuard.assertOrOverride(
                "alice", "alice", "Emergency: only admin available", true, ENTITY_TYPE, ENTITY_ID);
        assertThat(override).isTrue();
    }

    @Test
    void assertOrOverride_sameActorAdminBlankReason_throwsViolation() {
        assertThatThrownBy(() ->
                MakerCheckerGuard.assertOrOverride(
                        "alice", "alice", "   ", true, ENTITY_TYPE, ENTITY_ID))
                .isInstanceOf(MakerCheckerViolationException.class);
    }

    @Test
    void assertOrOverride_sameActorAdminNullReason_throwsViolation() {
        assertThatThrownBy(() ->
                MakerCheckerGuard.assertOrOverride(
                        "alice", "alice", null, true, ENTITY_TYPE, ENTITY_ID))
                .isInstanceOf(MakerCheckerViolationException.class);
    }

    @Test
    void assertOrOverride_sameActorAdminEmptyReason_throwsViolation() {
        assertThatThrownBy(() ->
                MakerCheckerGuard.assertOrOverride(
                        "alice", "alice", "", true, ENTITY_TYPE, ENTITY_ID))
                .isInstanceOf(MakerCheckerViolationException.class);
    }

    @Test
    void assertOrOverride_nullMaker_returnsFalse() {
        boolean override = MakerCheckerGuard.assertOrOverride(
                null, "bob", null, false, ENTITY_TYPE, ENTITY_ID);
        assertThat(override).isFalse();
    }

    @Test
    void assertOrOverride_nullChecker_returnsFalse() {
        boolean override = MakerCheckerGuard.assertOrOverride(
                "alice", null, null, false, ENTITY_TYPE, ENTITY_ID);
        assertThat(override).isFalse();
    }

    // --- Exception fields ---

    @Test
    void exceptionContainsEntityTypeAndId() {
        try {
            MakerCheckerGuard.assertDifferentActor("alice", "alice", ENTITY_TYPE, ENTITY_ID);
        } catch (MakerCheckerViolationException e) {
            assertThat(e.getEntityType()).isEqualTo(ENTITY_TYPE);
            assertThat(e.getEntityId()).isEqualTo(ENTITY_ID);
            return;
        }
        throw new AssertionError("Expected MakerCheckerViolationException");
    }
}
