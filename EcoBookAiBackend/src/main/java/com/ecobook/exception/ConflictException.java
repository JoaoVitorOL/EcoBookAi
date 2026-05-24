package com.ecobook.exception;

public class ConflictException extends RuntimeException {
    /**
     * Creates a new conflict exception with the provided message.
     * @param message exception message
     */
    public ConflictException(String message) {
        super(message);
    }

    /**
     * Creates a new conflict exception with the provided message and cause.
     * @param message exception message
     * @param cause underlying cause of the failure
     */
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
