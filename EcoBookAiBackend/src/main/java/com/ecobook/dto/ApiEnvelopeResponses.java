package com.ecobook.dto;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public final class ApiEnvelopeResponses {

    private ApiEnvelopeResponses() {
    }

    /**
     * Builds a successful API envelope response.
     * @param request request payload for the operation
     * @param message exception message
     * @param data notification or response data map
     * @return result of the operation
     */
    public static <T> ResponseEntity<ApiEnvelope<T>> ok(HttpServletRequest request, String message, T data) {
        return build(HttpStatus.OK, request, message, data);
    }

    /**
     * Builds a successful API envelope response.
     * @param request request payload for the operation
     * @param message exception message
     * @return result of the operation
     */
    public static ResponseEntity<ApiEnvelope<Void>> ok(HttpServletRequest request, String message) {
        return build(HttpStatus.OK, request, message, null);
    }

    /**
     * Builds a created API envelope response.
     * @param request request payload for the operation
     * @param message exception message
     * @param data notification or response data map
     * @return created result
     */
    public static <T> ResponseEntity<ApiEnvelope<T>> created(HttpServletRequest request, String message, T data) {
        return build(HttpStatus.CREATED, request, message, data);
    }

    public static ResponseEntity<ApiEnvelope<Void>> status(HttpStatus status,
                                                           HttpServletRequest request,
                                                           String message) {
        return build(status, request, message, null);
    }

    /**
     * Builds the status API envelope response.
     *
     * @param status the status filter value
     * @param request the request payload
     * @param message the message value
     * @param data the data value
     * @return the wrapped API response
     */
    public static <T> ResponseEntity<ApiEnvelope<T>> status(HttpStatus status,
                                                            HttpServletRequest request,
                                                            String message,
                                                            T data) {
        return build(status, request, message, data);
    }

    private static <T> ResponseEntity<ApiEnvelope<T>> build(HttpStatus status,
                                                            HttpServletRequest request,
                                                            String message,
                                                            T data) {
        return ResponseEntity.status(status).body(ApiEnvelope.<T>builder()
                .status(status.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .data(data)
                .build());
    }
}
