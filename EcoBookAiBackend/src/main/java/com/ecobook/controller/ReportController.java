package com.ecobook.controller;

import com.ecobook.annotation.RequireCompleteProfile;
import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import com.ecobook.dto.CreateNonReceiptReportRequestDTO;
import com.ecobook.dto.MaterialNonReceiptReportDTO;
import com.ecobook.service.ReportService;
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
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/materiais/{id}/nao-recebido")
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<MaterialNonReceiptReportDTO>> reportNonReceipt(
            @PathVariable String id,
            @RequestBody(required = false) CreateNonReceiptReportRequestDTO request,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        return ApiEnvelopeResponses.created(
                servletRequest,
                "Reporte de nao recebimento enviado com sucesso",
                reportService.reportNonReceipt(authentication.getName(), id, request)
        );
    }
}
