package com.ecobook.controller;

import com.ecobook.dto.AdminNonReceiptReportDTO;
import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import com.ecobook.dto.PagedResponseDTO;
import com.ecobook.dto.ResolveNonReceiptReportRequestDTO;
import com.ecobook.exception.BadRequestException;
import com.ecobook.model.enums.NonReceiptReportStatus;
import com.ecobook.service.AdminReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
            throw new BadRequestException("Os filtros de reportes sao invalidos", fieldErrors);
        }

        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Reportes carregados com sucesso",
                adminReportService.listReports(parsedStatus, PageRequest.of(page, size))
        );
    }

    @PatchMapping("/reports/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
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

    private void validatePagination(Integer page, Integer size, Map<String, String> fieldErrors) {
        if (page == null || page < 0) {
            fieldErrors.put("page", "Informe uma pagina maior ou igual a zero");
        }
        if (size == null || size < 1 || size > 100) {
            fieldErrors.put("size", "Informe um tamanho de pagina entre 1 e 100");
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
}
