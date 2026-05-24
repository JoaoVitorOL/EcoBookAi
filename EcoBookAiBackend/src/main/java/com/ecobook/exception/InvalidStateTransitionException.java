package com.ecobook.exception;

public class InvalidStateTransitionException extends RuntimeException {
    /**
     * Creates a new invalid-state-transition exception with the provided message.
     * @param message exception message
     */
    public InvalidStateTransitionException(String message) {
        super(message);
    }

    /**
     * Creates a new invalid-state-transition exception with the provided message and cause.
     * @param message exception message
     * @param cause underlying cause of the failure
     */
    public InvalidStateTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
