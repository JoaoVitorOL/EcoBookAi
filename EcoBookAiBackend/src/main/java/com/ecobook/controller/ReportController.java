package com.ecobook.controller;

import com.ecobook.annotation.RequireCompleteProfile;
import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import com.ecobook.dto.CreateNonReceiptReportRequestDTO;
import com.ecobook.dto.MaterialNonReceiptReportDTO;
import com.ecobook.service.ReportService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Fluxo de reporte de nao recebimento de material")
@SecurityRequirement(name = "bearer-jwt")
public class ReportController {

    private final ReportService reportService;

    /**
     * Handles the report non receipt request.
     *
     * @param id the resource identifier
     * @param request the request payload
     * @param authentication the authenticated principal context
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PostMapping("/materiais/{id}/nao-recebido")
    @RequireCompleteProfile
    @Operation(
            summary = "Reportar nao recebimento",
            description = "Abre um reporte quando o estudante aprovado informa que nao recebeu o material combinado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reporte criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Fluxo invalido para o material informado"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto ou acesso negado"),
            @ApiResponse(responseCode = "404", description = "Material ou solicitacao relacionada nao encontrada")
    })
    public ResponseEntity<ApiEnvelope<MaterialNonReceiptReportDTO>> reportNonReceipt(
            @PathVariable @Parameter(description = "Identificador do material reportado") String id,
            @RequestBody(required = false) CreateNonReceiptReportRequestDTO request,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        return ApiEnvelopeResponses.created(
                servletRequest,
                "Reporte de não recebimento enviado com sucesso",
                reportService.reportNonReceipt(authentication.getName(), id, request)
        );
    }
}
