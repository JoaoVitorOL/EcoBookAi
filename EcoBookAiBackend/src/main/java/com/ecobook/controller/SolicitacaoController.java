package com.ecobook.controller;

import com.ecobook.annotation.RequireCompleteProfile;
import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import com.ecobook.dto.SolicitacaoDTO;
import com.ecobook.model.enums.StatusSolicitacao;
import com.ecobook.service.SolicitacaoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class SolicitacaoController {

    private final SolicitacaoService solicitacaoService;

    @PostMapping("/materiais/{materialId}/solicitacoes")
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<SolicitacaoDTO>> createSolicitacao(@PathVariable String materialId,
                                                                         Authentication authentication,
                                                                         HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.status(
                HttpStatus.CREATED,
                servletRequest,
                "Solicitacao criada com sucesso",
                solicitacaoService.createRequest(authentication.getName(), materialId)
        );
    }

    @GetMapping("/solicitacoes/minhas")
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<List<SolicitacaoDTO>>> listMyRequests(
            @RequestParam(required = false) String status,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitacoes do estudante carregadas com sucesso",
                solicitacaoService.listCurrentUserRequests(
                        authentication.getName(),
                        parseStatus(status)
                )
        );
    }

    @GetMapping("/solicitacoes/pendentes")
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<List<SolicitacaoDTO>>> listPendingDonorRequests(
            Authentication authentication,
            HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitacoes pendentes do doador carregadas com sucesso",
                solicitacaoService.listPendingRequestsForDonor(authentication.getName())
        );
    }

    @GetMapping("/solicitacoes/aprovadas")
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<List<SolicitacaoDTO>>> listApprovedDonorRequests(
            Authentication authentication,
            HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitacoes aprovadas do doador carregadas com sucesso",
                solicitacaoService.listApprovedRequestsForDonor(authentication.getName())
        );
    }

    @GetMapping("/solicitacoes/{id}")
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<SolicitacaoDTO>> getRequest(@PathVariable String id,
                                                                  Authentication authentication,
                                                                  HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitacao carregada com sucesso",
                solicitacaoService.getRequest(authentication.getName(), id)
        );
    }

    @PatchMapping("/solicitacoes/{id}/aprovar")
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<SolicitacaoDTO>> approveRequest(@PathVariable String id,
                                                                      Authentication authentication,
                                                                      HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitacao aprovada com sucesso",
                solicitacaoService.approveRequest(authentication.getName(), id)
        );
    }

    @PatchMapping("/solicitacoes/{id}/recusar")
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<SolicitacaoDTO>> declineRequest(@PathVariable String id,
                                                                      Authentication authentication,
                                                                      HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitacao recusada com sucesso",
                solicitacaoService.declineRequest(authentication.getName(), id)
        );
    }

    @PatchMapping("/solicitacoes/{id}/cancelar")
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<SolicitacaoDTO>> cancelRequest(@PathVariable String id,
                                                                     Authentication authentication,
                                                                     HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitacao cancelada com sucesso",
                solicitacaoService.cancelRequest(authentication.getName(), id)
        );
    }

    @PatchMapping("/solicitacoes/{id}/concluir")
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<SolicitacaoDTO>> completeDonation(@PathVariable String id,
                                                                        Authentication authentication,
                                                                        HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Doacao concluida com sucesso",
                solicitacaoService.completeDonation(authentication.getName(), id)
        );
    }

    private StatusSolicitacao parseStatus(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return StatusSolicitacao.valueOf(rawValue.trim().toUpperCase(java.util.Locale.ROOT));
    }
}
