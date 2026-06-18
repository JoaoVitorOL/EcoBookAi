package com.ecobook.exception;

import java.util.Map;

public class UnprocessableEntityException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    /**
     * Creates a new unprocessable-entity exception with the provided message.
     * @param message exception message
     */
    public UnprocessableEntityException(String message) {
        this(message, null);
    }

    /**
     * Creates a new unprocessable-entity exception with optional field-level validation details.
     * @param message exception message
     * @param fieldErrors field-level validation errors
     */
    public UnprocessableEntityException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }

    /**
     * Returns the field-level validation errors attached to the exception.
     * @return field-level validation errors
     */
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
