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
@Tag(name = "Notificações", description = "Inbox persistida de notificações do usuário")
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
    @Operation(summary = "Listar notificações", description = "Retorna a inbox persistida de notificações do usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificações carregadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido"),
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
    @Operation(summary = "Marcar notificação como lida", description = "Marca uma notificação específica da inbox como lida.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificação marcada como lida"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto ou acesso negado"),
            @ApiResponse(responseCode = "404", description = "Notificação não encontrada")
    })
    public ResponseEntity<ApiEnvelope<Void>> markAsRead(
            @PathVariable @Parameter(description = "Identificador da notificação do usuário") String id,
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
    @Operation(summary = "Marcar todas como lidas", description = "Marca todas as notificações da inbox atual como lidas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificações marcadas como lidas"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido"),
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
