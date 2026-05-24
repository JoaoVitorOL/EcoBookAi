package com.ecobook.controller;

import com.ecobook.annotation.AuditAction;
import com.ecobook.dto.AuditLogDTO;
import com.ecobook.dto.AdminNonReceiptReportDTO;
import com.ecobook.dto.AdminUserSummaryDTO;
import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import com.ecobook.dto.MaterialDTO;
import com.ecobook.dto.PagedResponseDTO;
import com.ecobook.dto.ResolveNonReceiptReportRequestDTO;
import com.ecobook.exception.BadRequestException;
import com.ecobook.model.enums.NonReceiptReportStatus;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.service.AdminAuditService;
import com.ecobook.service.AdminPlatformService;
import com.ecobook.service.AdminReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Administracao", description = "Moderacao, auditoria e gestao administrativa da plataforma")
@SecurityRequirement(name = "bearer-jwt")
public class AdminController {

    private final AdminReportService adminReportService;
    private final AdminPlatformService adminPlatformService;
    private final AdminAuditService adminAuditService;

    /**
     * Handles the list reports request.
     *
     * @param status the status filter value
     * @param page the requested page index
     * @param size the requested page size
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar reportes", description = "Retorna os reportes administrativos de nao recebimento com paginacao e filtro opcional por status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reportes carregados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Filtros invalidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores")
    })
    public ResponseEntity<ApiEnvelope<PagedResponseDTO<AdminNonReceiptReportDTO>>> listReports(
            @RequestParam(required = false) @Parameter(description = "Filtro por status OPEN ou RESOLVED") String status,
            @RequestParam(defaultValue = "0") @Parameter(description = "Pagina offset") Integer page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Tamanho da pagina, entre 1 e 100") Integer size,
            HttpServletRequest servletRequest
    ) {
        LinkedHashMap<String, String> fieldErrors = new LinkedHashMap<>();
        NonReceiptReportStatus parsedStatus = parseStatus(status, fieldErrors);
        validatePagination(page, size, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new BadRequestException("Os filtros de reportes são inválidos", fieldErrors);
        }

        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Reportes carregados com sucesso",
                adminReportService.listReports(parsedStatus, PageRequest.of(page, size))
        );
    }

    /**
     * Handles the resolve report request.
     *
     * @param id the resource identifier
     * @param request the request payload
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PatchMapping("/reports/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditAction(action = "ADMIN_REPORT_RESOLVED", resourceType = "NON_RECEIPT_REPORT", resourceIdExpression = "#id")
    @Operation(
            summary = "Resolver reporte",
            description = "Resolve administrativamente um reporte aberto de nao recebimento.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Detalhes opcionais da resolucao administrativa"
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte resolvido com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores"),
            @ApiResponse(responseCode = "404", description = "Reporte nao encontrado")
    })
    public ResponseEntity<ApiEnvelope<AdminNonReceiptReportDTO>> resolveReport(
            @PathVariable @Parameter(description = "Identificador do reporte") String id,
            @RequestBody(required = false) ResolveNonReceiptReportRequestDTO request,
            HttpServletRequest servletRequest
    ) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Reporte resolvido com sucesso",
                adminReportService.resolveReport(id, request)
        );
    }

    /**
     * Handles the list materials request.
     *
     * @param status the status filter value
     * @param page the requested page index
     * @param size the requested page size
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @GetMapping("/materials")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar materiais para moderacao", description = "Lista os materiais da plataforma com filtro opcional por status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Materiais carregados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Filtros invalidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores")
    })
    public ResponseEntity<ApiEnvelope<PagedResponseDTO<MaterialDTO>>> listMaterials(
            @RequestParam(required = false) @Parameter(description = "Filtro por status do material") String status,
            @RequestParam(defaultValue = "0") @Parameter(description = "Pagina offset") Integer page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Tamanho da pagina, entre 1 e 100") Integer size,
            HttpServletRequest servletRequest
    ) {
        LinkedHashMap<String, String> fieldErrors = new LinkedHashMap<>();
        StatusMaterial parsedStatus = parseMaterialStatus(status, fieldErrors);
        validatePagination(page, size, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new BadRequestException("Os filtros de materiais são inválidos", fieldErrors);
        }

        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Materiais carregados com sucesso",
                adminPlatformService.listMaterials(parsedStatus, PageRequest.of(page, size))
        );
    }

    /**
     * Deletes a material through the administrative moderation surface.
     * @param id resource identifier
     * @return result of the operation
     */
    @DeleteMapping("/materials/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Excluir material como admin", description = "Remove um material da plataforma pelo painel administrativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Material excluido com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores"),
            @ApiResponse(responseCode = "404", description = "Material nao encontrado")
    })
    public ResponseEntity<Void> deleteMaterial(@PathVariable @Parameter(description = "Identificador do material") String id) {
        adminPlatformService.deleteMaterial(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Handles the list users request.
     *
     * @param page the requested page index
     * @param size the requested page size
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuarios", description = "Lista os usuarios da plataforma em formato resumido para auditoria e moderacao.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuarios carregados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Filtros invalidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores")
    })
    public ResponseEntity<ApiEnvelope<PagedResponseDTO<AdminUserSummaryDTO>>> listUsers(
            @RequestParam(defaultValue = "0") @Parameter(description = "Pagina offset") Integer page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Tamanho da pagina, entre 1 e 100") Integer size,
            HttpServletRequest servletRequest
    ) {
        LinkedHashMap<String, String> fieldErrors = new LinkedHashMap<>();
        validatePagination(page, size, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new BadRequestException("Os filtros de usuários são inválidos", fieldErrors);
        }

        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Usuarios carregados com sucesso",
                adminPlatformService.listUsers(PageRequest.of(page, size))
        );
    }

    /**
     * Handles the list audit logs request.
     *
     * @param actorUserId the actorUserId value
     * @param targetUserId the targetUserId value
     * @param action the action value
     * @param from the from value
     * @param to the to value
     * @param page the requested page index
     * @param size the requested page size
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @GetMapping("/audit-log")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar auditoria", description = "Consulta o log de auditoria com filtros por ator, alvo, acao e intervalo temporal.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Auditoria carregada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Filtros invalidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores")
    })
    public ResponseEntity<ApiEnvelope<PagedResponseDTO<AuditLogDTO>>> listAuditLogs(
            @RequestParam(required = false, name = "actor_user_id") @Parameter(description = "UUID do usuario ator") String actorUserId,
            @RequestParam(required = false, name = "target_user_id") @Parameter(description = "UUID do usuario alvo") String targetUserId,
            @RequestParam(required = false) @Parameter(description = "Filtro por nome da acao de auditoria") String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "Inicio do intervalo ISO-8601") java.time.LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "Fim do intervalo ISO-8601") java.time.LocalDateTime to,
            @RequestParam(defaultValue = "0") @Parameter(description = "Pagina offset") Integer page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Tamanho da pagina, entre 1 e 100") Integer size,
            HttpServletRequest servletRequest
    ) {
        LinkedHashMap<String, String> fieldErrors = new LinkedHashMap<>();
        validatePagination(page, size, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new BadRequestException("Os filtros de auditoria são inválidos", fieldErrors);
        }

        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Auditoria carregada com sucesso",
                adminAuditService.listAuditLogs(actorUserId, targetUserId, action, from, to, PageRequest.of(page, size))
        );
    }

    private void validatePagination(Integer page, Integer size, Map<String, String> fieldErrors) {
        if (page == null || page < 0) {
            fieldErrors.put("page", "Informe uma página maior ou igual a zero");
        }
        if (size == null || size < 1 || size > 100) {
            fieldErrors.put("size", "Informe um tamanho de página entre 1 e 100");
        }
    }

    private NonReceiptReportStatus parseStatus(String rawValue, Map<String, String> fieldErrors) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return NonReceiptReportStatus.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            fieldErrors.put("status", "Use um dos valores: OPEN ou RESOLVED");
            return null;
        }
    }

    private StatusMaterial parseMaterialStatus(String rawValue, Map<String, String> fieldErrors) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return StatusMaterial.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            fieldErrors.put("status", "Use um dos valores: DISPONIVEL, RESERVADO, DOADO ou CANCELADO");
            return null;
        }
    }
}
