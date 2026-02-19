package com.innercircle.sacco.common.guard;

import com.innercircle.sacco.common.exception.MakerCheckerViolationException;

import java.util.UUID;

/**
 * Enforces maker-checker (dual-authorization) controls on financial operations.
 * The person who creates a record cannot be the same person who approves it,
 * unless they are an ADMIN providing an override reason.
 */
public final class MakerCheckerGuard {

    private MakerCheckerGuard() {
    }

    /**
     * Assert that maker and checker are different actors.
     * Throws if they are the same person.
     *
     * @param maker      the actor who created the record
     * @param checker    the actor attempting to approve the record
     * @param entityType the type of entity being approved (e.g. "LoanApplication")
     * @param entityId   the ID of the entity being approved
     */
    public static void assertDifferentActor(String maker, String checker,
                                            String entityType, UUID entityId) {
        if (maker == null || checker == null) {
            return;
        }
        if (maker.equals(checker)) {
            throw new MakerCheckerViolationException(entityType, entityId);
        }
    }

    /**
     * Assert maker-checker separation, allowing ADMIN override with a documented reason.
     * Returns true if an override was used (caller should log OVERRIDE_APPROVED audit event).
     *
     * @param maker          the actor who created the record
     * @param checker        the actor attempting to approve the record
     * @param overrideReason optional reason for self-approval (required when same actor + admin)
     * @param isAdmin        whether the checker has ADMIN role
     * @param entityType     the type of entity being approved
     * @param entityId       the ID of the entity being approved
     * @return true if an ADMIN override was used, false if normal approval
     */
    public static boolean assertOrOverride(String maker, String checker,
                                           String overrideReason, boolean isAdmin,
                                           String entityType, UUID entityId) {
        if (maker == null || checker == null) {
            return false;
        }
        if (!maker.equals(checker)) {
            return false;
        }

        // Same actor — must be ADMIN with a reason
        if (!isAdmin) {
            throw new MakerCheckerViolationException(entityType, entityId);
        }
        if (overrideReason == null || overrideReason.isBlank()) {
            throw new MakerCheckerViolationException(entityType, entityId);
        }

        return true;
    }
}
