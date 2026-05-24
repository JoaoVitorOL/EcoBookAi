package com.ecobook.controller;

import com.ecobook.annotation.RequireCompleteProfile;
import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import com.ecobook.dto.UserNotificationDTO;
import com.ecobook.service.UserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/notificacoes")
@RequiredArgsConstructor
@Tag(name = "Notificacoes", description = "Inbox persistida de notificacoes do usuario")
@SecurityRequirement(name = "bearer-jwt")
public class NotificationController {

    private final UserNotificationService userNotificationService;

    /**
     * Handles the list notifications request.
     *
     * @param authentication the authenticated principal context
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @GetMapping
    @RequireCompleteProfile
    @Operation(summary = "Listar notificacoes", description = "Retorna a inbox persistida de notificacoes do usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificacoes carregadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto ou acesso negado")
    })
    public ResponseEntity<ApiEnvelope<List<UserNotificationDTO>>> listNotifications(
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Notificações carregadas com sucesso",
                userNotificationService.listCurrentUserNotifications(authentication.getName())
        );
    }

    /**
     * Handles the mark as read request.
     *
     * @param id the resource identifier
     * @param authentication the authenticated principal context
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PatchMapping("/{id}/ler")
    @RequireCompleteProfile
    @Operation(summary = "Marcar notificacao como lida", description = "Marca uma notificacao especifica da inbox como lida.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificacao marcada como lida"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto ou acesso negado"),
            @ApiResponse(responseCode = "404", description = "Notificacao nao encontrada")
    })
    public ResponseEntity<ApiEnvelope<Void>> markAsRead(
            @PathVariable @Parameter(description = "Identificador da notificacao do usuario") String id,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        userNotificationService.markAsRead(authentication.getName(), id);
        return ApiEnvelopeResponses.ok(servletRequest, "Notificação marcada como lida");
    }

    /**
     * Handles the mark all as read request.
     *
     * @param authentication the authenticated principal context
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PatchMapping("/ler-todas")
    @RequireCompleteProfile
    @Operation(summary = "Marcar todas como lidas", description = "Marca todas as notificacoes da inbox atual como lidas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificacoes marcadas como lidas"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto ou acesso negado")
    })
    public ResponseEntity<ApiEnvelope<Void>> markAllAsRead(
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        userNotificationService.markAllAsRead(authentication.getName());
        return ApiEnvelopeResponses.ok(servletRequest, "Notificações marcadas como lidas");
    }
}
