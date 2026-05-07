package com.ecobook.controller;

import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import com.ecobook.dto.FcmTokenRequestDTO;
import com.ecobook.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final UsuarioService usuarioService;

    @PostMapping("/tokens")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiEnvelope<Void>> registerToken(Authentication authentication,
                                                           @Valid @RequestBody FcmTokenRequestDTO request,
                                                           HttpServletRequest servletRequest) {
        usuarioService.updateFcmToken(authentication.getName(), request.getToken());
        return ApiEnvelopeResponses.ok(servletRequest, "Token FCM sincronizado com sucesso");
    }
}
