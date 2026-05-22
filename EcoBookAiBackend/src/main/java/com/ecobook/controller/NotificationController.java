package com.ecobook.controller;

import com.ecobook.annotation.RequireCompleteProfile;
import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import com.ecobook.dto.UserNotificationDTO;
import com.ecobook.service.UserNotificationService;
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
public class NotificationController {

    private final UserNotificationService userNotificationService;

    @GetMapping
    @RequireCompleteProfile
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

    @PatchMapping("/{id}/ler")
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<Void>> markAsRead(
            @PathVariable String id,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        userNotificationService.markAsRead(authentication.getName(), id);
        return ApiEnvelopeResponses.ok(servletRequest, "Notificação marcada como lida");
    }

    @PatchMapping("/ler-todas")
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<Void>> markAllAsRead(
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        userNotificationService.markAllAsRead(authentication.getName());
        return ApiEnvelopeResponses.ok(servletRequest, "Notificações marcadas como lidas");
    }
}
