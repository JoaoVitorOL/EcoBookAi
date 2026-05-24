package com.ecobook.exception;

public class ProfileIncompleteException extends RuntimeException {
    /**
     * Creates a new profile-incomplete exception with the provided message.
     * @param message exception message
     */
    public ProfileIncompleteException(String message) {
        super(message);
    }

    /**
     * Creates a new profile-incomplete exception with the provided message and cause.
     * @param message exception message
     * @param cause underlying cause of the failure
     */
    public ProfileIncompleteException(String message, Throwable cause) {
        super(message, cause);
    }
}
