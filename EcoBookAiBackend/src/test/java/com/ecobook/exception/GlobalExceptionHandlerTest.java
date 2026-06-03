package com.ecobook.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final HandlerAccessor handler = new HandlerAccessor();

    @Test
    @DisplayName("handleBadRequestException should preserve field-level errors")
    void shouldHandleBadRequestException() {
        var response = handler.handleBadRequestException(
                new BadRequestException("Payload inválido", Map.of("email", "Obrigatório")),
                request("/api/v1/auth/register")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("INVALID_FORMAT");
        assertThat(response.getBody().getFieldErrors()).containsEntry("email", "Obrigatório");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/auth/register");
    }

    @Test
    @DisplayName("handleAccessDeniedException should fallback to the default message when Spring provides a blank reason")
    void shouldHandleAccessDeniedWithDefaultMessage() {
        var response = handler.handleAccessDeniedException(
                new AccessDeniedException(" "),
                request("/api/v1/admin/reports")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getError()).isEqualTo("ACCESS_DENIED");
        assertThat(response.getBody().getMessage()).contains("perm");
    }

    @Test
    @DisplayName("handleAuthenticationException should return UNAUTHORIZED")
    void shouldHandleAuthenticationException() {
        var response = handler.handleAuthenticationException(
                new BadCredentialsException("Credenciais inválidas"),
                request("/api/v1/auth/login")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getError()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getBody().getMessage()).isEqualTo("Credenciais inválidas");
    }

    @Test
    @DisplayName("handleUnprocessableEntityException should propagate field errors")
    void shouldHandleUnprocessableEntityException() {
        var response = handler.handleUnprocessableEntityException(
                new UnprocessableEntityException("Estado inválido", Map.of("status", "Transição inválida")),
                request("/api/v1/solicitacoes/1/aprovar")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().getFieldErrors()).containsEntry("status", "Transição inválida");
    }

    @Test
    @DisplayName("handlePayloadTooLargeException should map explicit size failures")
    void shouldHandlePayloadTooLargeException() {
        var response = handler.handlePayloadTooLargeException(
                new PayloadTooLargeException("Arquivo excede 5MB"),
                request("/api/v1/materiais/preview")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody().getError()).isEqualTo("PAYLOAD_TOO_LARGE");
    }

    @Test
    @DisplayName("handleMaxUploadSizeExceededException should normalize multipart errors to the same 5MB contract")
    void shouldHandleMultipartMaxUploadExceeded() {
        var response = handler.handleMaxUploadSizeExceededExceptionPublic(
                new MaxUploadSizeExceededException(5_000_000),
                HttpHeaders.EMPTY,
                HttpStatus.PAYLOAD_TOO_LARGE,
                request("/api/v1/materiais/preview")
        );

        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(body.getMessage()).contains("5MB").contains("arquivo menor");
    }

    @Test
    @DisplayName("handleMethodArgumentNotValid should flatten field errors into the API response")
    void shouldHandleMethodArgumentNotValid() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new SampleRequest(), "sampleRequest");
        bindingResult.addError(new FieldError("sampleRequest", "email", "E-mail inválido"));
        bindingResult.addError(new FieldError("sampleRequest", "password", "Senha obrigatória"));

        Method method = SampleController.class.getDeclaredMethod("submit", SampleRequest.class);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        var response = handler.handleMethodArgumentNotValidPublic(
                exception,
                HttpHeaders.EMPTY,
                HttpStatus.BAD_REQUEST,
                request("/api/v1/auth/register")
        );

        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getError()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.getFieldErrors())
                .containsEntry("email", "E-mail inválido")
                .containsEntry("password", "Senha obrigatória");
    }

    @Test
    @DisplayName("handleGenericException should hide internal details from the client")
    void shouldHandleGenericException() {
        var response = handler.handleGenericException(
                new IllegalStateException("boom"),
                request("/api/v1/health")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("Ocorreu um erro inesperado");
    }

    private WebRequest request(String path) {
        return new ServletWebRequest(new MockHttpServletRequest("GET", path));
    }

    private static final class HandlerAccessor extends GlobalExceptionHandler {
        org.springframework.http.ResponseEntity<Object> handleMaxUploadSizeExceededExceptionPublic(
                MaxUploadSizeExceededException ex,
                HttpHeaders headers,
                org.springframework.http.HttpStatusCode status,
                WebRequest request
        ) {
            return super.handleMaxUploadSizeExceededException(ex, headers, status, request);
        }

        org.springframework.http.ResponseEntity<Object> handleMethodArgumentNotValidPublic(
                MethodArgumentNotValidException ex,
                HttpHeaders headers,
                org.springframework.http.HttpStatusCode status,
                WebRequest request
        ) {
            return super.handleMethodArgumentNotValid(ex, headers, status, request);
        }
    }

    @SuppressWarnings("unused")
    private static final class SampleController {
        public void submit(SampleRequest request) {
        }
    }

    private static final class SampleRequest {
        private String email;
        private String password;
    }
}
