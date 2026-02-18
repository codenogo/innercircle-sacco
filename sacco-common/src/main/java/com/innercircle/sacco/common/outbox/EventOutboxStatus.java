package com.innercircle.sacco.common.outbox;

public enum EventOutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED
}
