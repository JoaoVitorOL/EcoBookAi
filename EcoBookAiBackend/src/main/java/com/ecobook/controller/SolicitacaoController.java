package com.ecobook.controller;

import com.ecobook.annotation.RequireCompleteProfile;
import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import com.ecobook.dto.SolicitacaoDTO;
import com.ecobook.exception.BadRequestException;
import com.ecobook.model.enums.StatusSolicitacao;
import com.ecobook.service.SolicitacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Solicitacoes", description = "Fluxo transacional entre estudante e doador")
@SecurityRequirement(name = "bearer-jwt")
public class SolicitacaoController {

    private final SolicitacaoService solicitacaoService;

    /**
     * Handles the create solicitacao request.
     *
     * @param materialId the material identifier
     * @param authentication the authenticated principal context
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PostMapping("/materiais/{materialId}/solicitacoes")
    @RequireCompleteProfile
    @Operation(summary = "Criar solicitacao", description = "Cria uma solicitacao do estudante para um material disponivel.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitacao criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Material proprio ou fluxo invalido"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto"),
            @ApiResponse(responseCode = "404", description = "Material nao encontrado"),
            @ApiResponse(responseCode = "409", description = "Solicitacao duplicada ou material indisponivel")
    })
    public ResponseEntity<ApiEnvelope<SolicitacaoDTO>> createSolicitacao(@PathVariable @Parameter(description = "Identificador do material solicitado") String materialId,
                                                                         Authentication authentication,
                                                                         HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.status(
                HttpStatus.CREATED,
                servletRequest,
                "Solicitacao criada com sucesso",
                solicitacaoService.createRequest(authentication.getName(), materialId)
        );
    }

    /**
     * Handles the list my requests request.
     *
     * @param status the status filter value
     * @param authentication the authenticated principal context
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @GetMapping("/solicitacoes/minhas")
    @RequireCompleteProfile
    @Operation(summary = "Listar solicitacoes do estudante", description = "Retorna as solicitacoes do usuario autenticado, com filtro opcional por status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitacoes carregadas com sucesso"),
            @ApiResponse(responseCode = "400", description = "Status invalido"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto")
    })
    public ResponseEntity<ApiEnvelope<List<SolicitacaoDTO>>> listMyRequests(
            @RequestParam(required = false) @Parameter(description = "Filtro opcional por status da solicitacao") String status,
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

    /**
     * Handles the list pending donor requests request.
     *
     * @param authentication the authenticated principal context
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @GetMapping("/solicitacoes/pendentes")
    @RequireCompleteProfile
    @Operation(summary = "Listar pendentes do doador", description = "Retorna as solicitacoes pendentes recebidas pelo doador autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitacoes pendentes carregadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto")
    })
    public ResponseEntity<ApiEnvelope<List<SolicitacaoDTO>>> listPendingDonorRequests(
            Authentication authentication,
            HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitacoes pendentes do doador carregadas com sucesso",
                solicitacaoService.listPendingRequestsForDonor(authentication.getName())
        );
    }

    /**
     * Handles the list approved donor requests request.
     *
     * @param authentication the authenticated principal context
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @GetMapping("/solicitacoes/aprovadas")
    @RequireCompleteProfile
    @Operation(summary = "Listar aprovadas do doador", description = "Retorna as solicitacoes aprovadas ainda ativas para o doador autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitacoes aprovadas carregadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto")
    })
    public ResponseEntity<ApiEnvelope<List<SolicitacaoDTO>>> listApprovedDonorRequests(
            Authentication authentication,
            HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitacoes aprovadas do doador carregadas com sucesso",
                solicitacaoService.listApprovedRequestsForDonor(authentication.getName())
        );
    }

    /**
     * Handles the get request request.
     *
     * @param id the resource identifier
     * @param authentication the authenticated principal context
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @GetMapping("/solicitacoes/{id}")
    @RequireCompleteProfile
    @Operation(summary = "Consultar solicitacao", description = "Retorna os detalhes de uma solicitacao acessivel ao usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitacao carregada com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto"),
            @ApiResponse(responseCode = "404", description = "Solicitacao nao encontrada")
    })
    public ResponseEntity<ApiEnvelope<SolicitacaoDTO>> getRequest(@PathVariable @Parameter(description = "Identificador da solicitacao") String id,
                                                                  Authentication authentication,
                                                                  HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitacao carregada com sucesso",
                solicitacaoService.getRequest(authentication.getName(), id)
        );
    }

    /**
     * Handles the approve request request.
     *
     * @param id the resource identifier
     * @param authentication the authenticated principal context
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PatchMapping("/solicitacoes/{id}/aprovar")
    @RequireCompleteProfile
    @Operation(summary = "Aprovar solicitacao", description = "Aprova uma solicitacao pendente e reserva o material para o estudante escolhido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitacao aprovada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Fluxo invalido para aprovacao"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto"),
            @ApiResponse(responseCode = "404", description = "Solicitacao nao encontrada"),
            @ApiResponse(responseCode = "409", description = "Corrida de aprovacao ou estado concorrente")
    })
    public ResponseEntity<ApiEnvelope<SolicitacaoDTO>> approveRequest(@PathVariable @Parameter(description = "Identificador da solicitacao") String id,
                                                                      Authentication authentication,
                                                                      HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitacao aprovada com sucesso",
                solicitacaoService.approveRequest(authentication.getName(), id)
        );
    }

    /**
     * Handles the decline request request.
     *
     * @param id the resource identifier
     * @param authentication the authenticated principal context
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PatchMapping("/solicitacoes/{id}/recusar")
    @RequireCompleteProfile
    @Operation(summary = "Recusar solicitacao", description = "Recusa uma solicitacao pendente do material.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitacao recusada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Fluxo invalido para recusa"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto"),
            @ApiResponse(responseCode = "404", description = "Solicitacao nao encontrada")
    })
    public ResponseEntity<ApiEnvelope<SolicitacaoDTO>> declineRequest(@PathVariable @Parameter(description = "Identificador da solicitacao") String id,
                                                                      Authentication authentication,
                                                                      HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitacao recusada com sucesso",
                solicitacaoService.declineRequest(authentication.getName(), id)
        );
    }

    /**
     * Handles the cancel request request.
     *
     * @param id the resource identifier
     * @param authentication the authenticated principal context
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PatchMapping("/solicitacoes/{id}/cancelar")
    @RequireCompleteProfile
    @Operation(summary = "Cancelar solicitacao", description = "Cancela uma solicitacao do estudante ou do doador quando o fluxo permitir.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitacao cancelada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Fluxo invalido para cancelamento"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto"),
            @ApiResponse(responseCode = "404", description = "Solicitacao nao encontrada")
    })
    public ResponseEntity<ApiEnvelope<SolicitacaoDTO>> cancelRequest(@PathVariable @Parameter(description = "Identificador da solicitacao") String id,
                                                                     Authentication authentication,
                                                                     HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitacao cancelada com sucesso",
                solicitacaoService.cancelRequest(authentication.getName(), id)
        );
    }

    /**
     * Handles the complete donation request.
     *
     * @param id the resource identifier
     * @param authentication the authenticated principal context
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PatchMapping("/solicitacoes/{id}/concluir")
    @RequireCompleteProfile
    @Operation(summary = "Concluir doacao", description = "Conclui a doacao aprovada e marca o material como doado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Doacao concluida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Fluxo invalido para conclusao"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto"),
            @ApiResponse(responseCode = "404", description = "Solicitacao nao encontrada")
    })
    public ResponseEntity<ApiEnvelope<SolicitacaoDTO>> completeDonation(@PathVariable @Parameter(description = "Identificador da solicitacao") String id,
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

        try {
            return StatusSolicitacao.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(
                    "O filtro de status informado e invalido",
                    Map.of("status", "Use um dos valores: PENDENTE, APROVADA, RECUSADA, CANCELADA, CONCLUIDA")
            );
        }
    }
}
