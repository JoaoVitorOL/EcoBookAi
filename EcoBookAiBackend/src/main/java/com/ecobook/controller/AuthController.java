package com.ecobook.controller;

import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import com.ecobook.dto.AuthResponseDTO;
import com.ecobook.dto.LoginRequestDTO;
import com.ecobook.dto.RegisterRequestDTO;
import com.ecobook.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiEnvelope<AuthResponseDTO>> register(@Valid @RequestBody RegisterRequestDTO request,
                                                                HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.created(
                servletRequest,
                "Conta criada com sucesso",
                authService.register(request)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiEnvelope<AuthResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request,
                                                             HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Autenticacao realizada com sucesso",
                authService.login(request)
        );
    }
}
