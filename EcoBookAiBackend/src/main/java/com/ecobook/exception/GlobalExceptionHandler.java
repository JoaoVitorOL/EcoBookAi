package com.ecobook.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(404)
                .error("NOT_FOUND")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(404).body(errorResponse);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(
            ConflictException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(409)
                .error("CONFLICT")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        log.warn("Conflict error: {}", ex.getMessage());
        return ResponseEntity.status(409).body(errorResponse);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStateTransitionException(
            InvalidStateTransitionException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(422)
                .error("INVALID_STATE_TRANSITION")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        log.warn("Invalid state transition: {}", ex.getMessage());
        return ResponseEntity.status(422).body(errorResponse);
    }

    @ExceptionHandler(ProfileIncompleteException.class)
    public ResponseEntity<ErrorResponse> handleProfileIncompleteException(
            ProfileIncompleteException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(403)
                .error("INCOMPLETE_PROFILE")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        log.warn("Profile incomplete: {}", ex.getMessage());
        return ResponseEntity.status(403).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(403)
                .error("ACCESS_DENIED")
                .message("You do not have permission to access this resource")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(403).body(errorResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(401)
                .error("UNAUTHORIZED")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        log.warn("Authentication error: {}", ex.getMessage());
        return ResponseEntity.status(401).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(400)
                .error("INVALID_FORMAT")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.status(400).body(errorResponse);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(400)
                .error("INVALID_FORMAT")
                .message(ex.getMessage())
                .fieldErrors(ex.getFieldErrors())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(400).body(errorResponse);
    }

    @ExceptionHandler(UnprocessableEntityException.class)
    public ResponseEntity<ErrorResponse> handleUnprocessableEntityException(
            UnprocessableEntityException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(422)
                .error("UNPROCESSABLE_ENTITY")
                .message(ex.getMessage())
                .fieldErrors(ex.getFieldErrors())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        log.warn("Unprocessable entity: {}", ex.getMessage());
        return ResponseEntity.status(422).body(errorResponse);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(400)
                .error("VALIDATION_ERROR")
                .message("Validation failed")
                .fieldErrors(errors)
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        log.warn("Validation error: {}", errors);
        return ResponseEntity.status(400).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(500)
                .error("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        log.error("Unexpected error", ex);
        return ResponseEntity.status(500).body(errorResponse);
    }
}
