package com.innercircle.sacco.common.outbox;

public enum EventDeadLetterStatus {
    PENDING_RETRY,
    RETRYING,
    RESOLVED,
    FAILED
}
