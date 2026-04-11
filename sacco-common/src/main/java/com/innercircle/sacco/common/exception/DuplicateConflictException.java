package com.innercircle.sacco.common.exception;

public class DuplicateConflictException extends RuntimeException {

    public DuplicateConflictException(String message) {
        super(message);
    }

    public DuplicateConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
