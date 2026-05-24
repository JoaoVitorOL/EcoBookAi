package com.ecobook.exception;

public class PayloadTooLargeException extends RuntimeException {
    /**
     * Creates a new payload-too-large exception with the provided message.
     * @param message exception message
     */
    public PayloadTooLargeException(String message) {
        super(message);
    }
}
