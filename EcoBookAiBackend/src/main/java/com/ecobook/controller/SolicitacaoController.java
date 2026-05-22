package com.ecobook.controller;

import com.ecobook.annotation.RequireCompleteProfile;
import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import com.ecobook.dto.SolicitacaoDTO;
import com.ecobook.exception.BadRequestException;
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
import java.util.Locale;
import java.util.Map;

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
                "Solicitação criada com sucesso",
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
                "Solicitações do estudante carregadas com sucesso",
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
                "Solicitações pendentes do doador carregadas com sucesso",
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
                "Solicitações aprovadas do doador carregadas com sucesso",
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
                "Solicitação carregada com sucesso",
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
                "Solicitação aprovada com sucesso",
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
                "Solicitação recusada com sucesso",
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
                "Solicitação cancelada com sucesso",
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
                "Doação concluída com sucesso",
                solicitacaoService.completeDonation(authentication.getName(), id)
        );
    }

    private StatusSolicitacao parseStatus(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return StatusSolicitacao.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(
                    "O filtro de status informado é inválido",
                    Map.of("status", "Use um dos valores: PENDENTE, APROVADA, RECUSADA, CANCELADA, CONCLUÍDA")
            );
        }
    }
}
