package com.innercircle.sacco.common.exception;

public class InvalidStateTransitionException extends BusinessException {

    private final String currentState;
    private final String targetState;
    private final String entityType;

    public InvalidStateTransitionException(String entityType, String currentState, String targetState) {
        super("Invalid state transition for " + entityType + ": " + currentState + " -> " + targetState);
        this.entityType = entityType;
        this.currentState = currentState;
        this.targetState = targetState;
    }

    public String getCurrentState() {
        return currentState;
    }

    public String getTargetState() {
        return targetState;
    }

    public String getEntityType() {
        return entityType;
    }
}
