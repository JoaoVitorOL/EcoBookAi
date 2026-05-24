package com.ecobook.exception;

import java.util.Map;

public class BadRequestException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    /**
     * Creates a new bad-request exception with field-level validation details.
     * @param message exception message
     * @param fieldErrors field-level validation errors
     */
    public BadRequestException(String message, Map<String, String> fieldErrors) {
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
