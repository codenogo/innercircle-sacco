package com.innercircle.sacco.common.exception;

import java.util.UUID;

public class MakerCheckerViolationException extends RuntimeException {

    private final String entityType;
    private final UUID entityId;

    public MakerCheckerViolationException(String entityType, UUID entityId) {
        super("Maker-checker violation: the same user cannot create and approve a " + entityType);
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }
}
