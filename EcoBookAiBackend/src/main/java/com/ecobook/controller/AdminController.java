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
public class AdminController {

    private final AdminReportService adminReportService;
    private final AdminPlatformService adminPlatformService;
    private final AdminAuditService adminAuditService;

    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiEnvelope<PagedResponseDTO<AdminNonReceiptReportDTO>>> listReports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
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

    @PatchMapping("/reports/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditAction(action = "ADMIN_REPORT_RESOLVED", resourceType = "NON_RECEIPT_REPORT", resourceIdExpression = "#id")
    public ResponseEntity<ApiEnvelope<AdminNonReceiptReportDTO>> resolveReport(
            @PathVariable String id,
            @RequestBody(required = false) ResolveNonReceiptReportRequestDTO request,
            HttpServletRequest servletRequest
    ) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Reporte resolvido com sucesso",
                adminReportService.resolveReport(id, request)
        );
    }

    @GetMapping("/materials")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiEnvelope<PagedResponseDTO<MaterialDTO>>> listMaterials(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
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

    @DeleteMapping("/materials/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMaterial(@PathVariable String id) {
        adminPlatformService.deleteMaterial(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiEnvelope<PagedResponseDTO<AdminUserSummaryDTO>>> listUsers(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
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

    @GetMapping("/audit-log")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiEnvelope<PagedResponseDTO<AuditLogDTO>>> listAuditLogs(
            @RequestParam(required = false, name = "actor_user_id") String actorUserId,
            @RequestParam(required = false, name = "target_user_id") String targetUserId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
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
