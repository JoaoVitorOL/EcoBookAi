package com.ecobook.dto;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public final class ApiEnvelopeResponses {

    private ApiEnvelopeResponses() {
    }

    public static <T> ResponseEntity<ApiEnvelope<T>> ok(HttpServletRequest request, String message, T data) {
        return build(HttpStatus.OK, request, message, data);
    }

    public static ResponseEntity<ApiEnvelope<Void>> ok(HttpServletRequest request, String message) {
        return build(HttpStatus.OK, request, message, null);
    }

    public static <T> ResponseEntity<ApiEnvelope<T>> created(HttpServletRequest request, String message, T data) {
        return build(HttpStatus.CREATED, request, message, data);
    }

    public static ResponseEntity<ApiEnvelope<Void>> status(HttpStatus status,
                                                           HttpServletRequest request,
                                                           String message) {
        return build(status, request, message, null);
    }

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
