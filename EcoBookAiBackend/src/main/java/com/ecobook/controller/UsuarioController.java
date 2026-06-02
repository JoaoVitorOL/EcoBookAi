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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Perfil, consentimentos, exportacao e exclusao de conta")
@SecurityRequirement(name = "bearer-jwt")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UserDeletionService userDeletionService;
    private final UserDataExportService userDataExportService;

    /**
     * Handles the get me request.
     *
     * @param authentication the authenticated principal context
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Consultar perfil atual", description = "Retorna o snapshot do perfil do usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil carregado com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido")
    })
    public ResponseEntity<ApiEnvelope<UsuarioDTO>> getMe(Authentication authentication,
                                                         HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Perfil carregado com sucesso",
                usuarioService.getByEmail(authentication.getName())
        );
    }

    /**
     * Handles the update me request.
     *
     * @param authentication the authenticated principal context
     * @param request the request payload
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PutMapping("/me")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Atualizar perfil",
            description = "Atualiza os dados principais do perfil, completa o onboarding quando os campos obrigatorios forem atendidos e pode exigir novo login se o email for alterado.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payload completo ou parcial de edicao do perfil"
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Email duplicado ou formato invalido"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "422", description = "Campos obrigatorios ausentes ou invalidos")
    })
    public ResponseEntity<ApiEnvelope<UsuarioDTO>> updateMe(Authentication authentication,
                                                            @Valid @RequestBody UpdateProfileRequestDTO request,
                                                            HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Perfil atualizado com sucesso",
                usuarioService.updateProfile(authentication.getName(), request)
        );
    }

    @PostMapping(value = "/me/foto-perfil", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Atualizar foto de perfil", description = "Substitui a foto de perfil atual do usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Foto de perfil atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Imagem invalida"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido")
    })
    public ResponseEntity<ApiEnvelope<UsuarioDTO>> updateProfilePhoto(Authentication authentication,
                                                                      @RequestPart("image") MultipartFile image,
                                                                      HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Foto de perfil atualizada com sucesso",
                usuarioService.updateProfilePhoto(authentication.getName(), image)
        );
    }

    @GetMapping("/{userId}/foto-perfil")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Baixar foto de perfil", description = "Retorna a foto de perfil publicada para o usuario informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Foto de perfil carregada com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "404", description = "Foto de perfil nao encontrada")
    })
    public ResponseEntity<Resource> getProfilePhoto(@PathVariable String userId,
                                                    Authentication authentication) {
        UsuarioService.ProfilePhotoPayload payload = usuarioService.loadProfilePhoto(authentication.getName(), userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .body(payload.resource());
    }

    /**
     * Handles the update ai consent request.
     *
     * @param authentication the authenticated principal context
     * @param request the request payload
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PatchMapping("/me/consentimento-ia")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Atualizar consentimento de IA",
            description = "Ativa ou desativa o consentimento de IA sem reenviar o restante do perfil.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Novo estado do consentimento de IA"
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consentimento atualizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "422", description = "Payload invalido")
    })
    public ResponseEntity<ApiEnvelope<UsuarioDTO>> updateAiConsent(Authentication authentication,
                                                                   @Valid @RequestBody UpdateAiConsentRequestDTO request,
                                                                   HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Consentimento de IA atualizado com sucesso",
                usuarioService.updateAiConsent(authentication.getName(), Boolean.TRUE.equals(request.getConsentimentoIa()))
        );
    }

    /**
     * Handles the update ai consent alias request.
     *
     * @param authentication the authenticated principal context
     * @param request the request payload
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PatchMapping("/me/consent")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Atualizar consentimento de IA via alias", description = "Alias compatível do endpoint de consentimento de IA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consentimento atualizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "422", description = "Payload invalido")
    })
    public ResponseEntity<ApiEnvelope<UsuarioDTO>> updateAiConsentAlias(Authentication authentication,
                                                                        @Valid @RequestBody UpdateAiConsentRequestDTO request,
                                                                        HttpServletRequest servletRequest) {
        return updateAiConsent(authentication, request, servletRequest);
    }

    /**
     * Handles the revoke ai consent request.
     *
     * @param authentication the authenticated principal context
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @DeleteMapping("/me/consent/ai-classification")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Revogar consentimento de IA", description = "Revoga o consentimento de IA e retorna o perfil atualizado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consentimento revogado com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido")
    })
    public ResponseEntity<ApiEnvelope<UsuarioDTO>> revokeAiConsent(Authentication authentication,
                                                                   HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Consentimento de IA revogado com sucesso",
                usuarioService.updateAiConsent(authentication.getName(), false)
        );
    }

    /**
     * Handles the get consent status request.
     *
     * @param authentication the authenticated principal context
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @GetMapping("/me/consent")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Consultar status de consentimento", description = "Retorna o resumo do consentimento atual do usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status carregado com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido")
    })
    public ResponseEntity<ApiEnvelope<UserConsentStatusDTO>> getConsentStatus(Authentication authentication,
                                                                              HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Status de consentimento carregado com sucesso",
                usuarioService.getConsentStatus(authentication.getName())
        );
    }

    /**
     * Handles the delete account request.
     *
     * @param authentication the authenticated principal context
     * @param request the request payload
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PostMapping("/delete")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Excluir conta",
            description = "Executa a exclusao LGPD da conta, anonimiza dados e revoga o token atual.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Confirmacao da exclusao da conta"
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta removida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Confirmacao ausente ou invalida"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido")
    })
    public ResponseEntity<ApiEnvelope<DeleteAccountResponseDTO>> deleteAccount(Authentication authentication,
                                                                               @Valid @RequestBody DeleteAccountRequestDTO request,
                                                                               HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Conta removida com sucesso",
                userDeletionService.deleteCurrentUser(authentication.getName(), extractBearerToken(servletRequest), request)
        );
    }

    /**
     * Exports the authenticated user's personal data package.
     * @param authentication current authentication context
     * @return result of the operation
     */
    @PostMapping("/me/export")
    @PreAuthorize("hasRole('USER')")
    @AuditAction(action = "ACCOUNT_EXPORT_REQUESTED", resourceType = "USER_EXPORT")
    @Operation(summary = "Exportar dados pessoais", description = "Gera um arquivo com os dados pessoais exportaveis do usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo gerado com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido")
    })
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
