package com.ecobook.controller;

import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import com.ecobook.dto.FcmTokenRequestDTO;
import com.ecobook.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "FCM", description = "Sincronizacao do token do dispositivo para notificacoes push")
@SecurityRequirement(name = "bearer-jwt")
public class FcmController {

    private final UsuarioService usuarioService;

    /**
     * Handles the register token request.
     *
     * @param authentication the authenticated principal context
     * @param request the request payload
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PostMapping("/tokens")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Registrar token FCM",
            description = "Associa o token FCM atual do dispositivo ao usuario autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token sincronizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Usuario sem permissao"),
            @ApiResponse(responseCode = "422", description = "Token ausente ou invalido")
    })
    public ResponseEntity<ApiEnvelope<Void>> registerToken(Authentication authentication,
                                                           @Valid @RequestBody FcmTokenRequestDTO request,
                                                           HttpServletRequest servletRequest) {
        usuarioService.updateFcmToken(authentication.getName(), request.getToken());
        return ApiEnvelopeResponses.ok(servletRequest, "Token FCM sincronizado com sucesso");
    }
}
