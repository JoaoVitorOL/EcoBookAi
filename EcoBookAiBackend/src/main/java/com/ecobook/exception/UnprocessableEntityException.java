package com.ecobook.exception;

import java.util.Map;

public class UnprocessableEntityException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public UnprocessableEntityException(String message) {
        this(message, null);
    }

    public UnprocessableEntityException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
