package com.ecobook.controller;

import com.ecobook.annotation.RequireCompleteProfile;
import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/solicitacoes")
public class SolicitacaoController {

    @PostMapping
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<Void>> createSolicitacao(HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.status(
                HttpStatus.NOT_IMPLEMENTED,
                servletRequest,
                "Fluxo de solicitacoes sera implementado nas proximas fases"
        );
    }
}
