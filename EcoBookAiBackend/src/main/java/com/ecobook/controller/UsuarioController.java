package com.ecobook.controller;

import com.ecobook.annotation.AuditAction;
import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import com.ecobook.dto.DeleteAccountRequestDTO;
import com.ecobook.dto.DeleteAccountResponseDTO;
import com.ecobook.dto.UpdateAiConsentRequestDTO;
import com.ecobook.dto.UpdateProfileRequestDTO;
import com.ecobook.dto.UsuarioDTO;
import com.ecobook.dto.UserConsentStatusDTO;
import com.ecobook.service.UserDataExportService;
import com.ecobook.service.UserDeletionService;
import com.ecobook.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UserDeletionService userDeletionService;
    private final UserDataExportService userDataExportService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiEnvelope<UsuarioDTO>> getMe(Authentication authentication,
                                                         HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Perfil carregado com sucesso",
                usuarioService.getByEmail(authentication.getName())
        );
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiEnvelope<UsuarioDTO>> updateMe(Authentication authentication,
                                                            @Valid @RequestBody UpdateProfileRequestDTO request,
                                                            HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Perfil atualizado com sucesso",
                usuarioService.updateProfile(authentication.getName(), request)
        );
    }

    @PatchMapping("/me/consentimento-ia")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiEnvelope<UsuarioDTO>> updateAiConsent(Authentication authentication,
                                                                   @Valid @RequestBody UpdateAiConsentRequestDTO request,
                                                                   HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Consentimento de IA atualizado com sucesso",
                usuarioService.updateAiConsent(authentication.getName(), Boolean.TRUE.equals(request.getConsentimentoIa()))
        );
    }

    @PatchMapping("/me/consent")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiEnvelope<UsuarioDTO>> updateAiConsentAlias(Authentication authentication,
                                                                        @Valid @RequestBody UpdateAiConsentRequestDTO request,
                                                                        HttpServletRequest servletRequest) {
        return updateAiConsent(authentication, request, servletRequest);
    }

    @DeleteMapping("/me/consent/ai-classification")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiEnvelope<UsuarioDTO>> revokeAiConsent(Authentication authentication,
                                                                   HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Consentimento de IA revogado com sucesso",
                usuarioService.updateAiConsent(authentication.getName(), false)
        );
    }

    @GetMapping("/me/consent")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiEnvelope<UserConsentStatusDTO>> getConsentStatus(Authentication authentication,
                                                                              HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Status de consentimento carregado com sucesso",
                usuarioService.getConsentStatus(authentication.getName())
        );
    }

    @PostMapping("/delete")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiEnvelope<DeleteAccountResponseDTO>> deleteAccount(Authentication authentication,
                                                                               @Valid @RequestBody DeleteAccountRequestDTO request,
                                                                               HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Conta removida com sucesso",
                userDeletionService.deleteCurrentUser(authentication.getName(), extractBearerToken(servletRequest), request)
        );
    }

    @PostMapping("/me/export")
    @PreAuthorize("hasRole('USER')")
    @AuditAction(action = "ACCOUNT_EXPORT_REQUESTED", resourceType = "USER_EXPORT")
    public ResponseEntity<ByteArrayResource> exportPersonalData(Authentication authentication) {
        UserDataExportService.ExportedUserData export = userDataExportService.exportCurrentUser(authentication.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.fileName() + "\"")
                .body(new ByteArrayResource(export.bytes()));
    }

    private String extractBearerToken(HttpServletRequest servletRequest) {
        String header = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
