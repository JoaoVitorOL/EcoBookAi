package com.ecobook.exception;

public class ResourceNotFoundException extends RuntimeException {
    /**
     * Creates a new resource-not-found exception with the provided message.
     * @param message exception message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a new resource-not-found exception with the provided message and cause.
     * @param message exception message
     * @param cause underlying cause of the failure
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
