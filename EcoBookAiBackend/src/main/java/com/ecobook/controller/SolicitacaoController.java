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
@Tag(name = "Solicitações", description = "Fluxo transacional entre estudante e doador")
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
    @Operation(summary = "Criar solicitação", description = "Cria uma solicitação do estudante para um material disponível.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitação criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Material próprio ou fluxo inválido"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto"),
            @ApiResponse(responseCode = "404", description = "Material não encontrado"),
            @ApiResponse(responseCode = "409", description = "Solicitação duplicada ou material indisponível")
    })
    public ResponseEntity<ApiEnvelope<SolicitacaoDTO>> createSolicitacao(
            @PathVariable @Parameter(description = "Identificador do material solicitado") String materialId,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        return ApiEnvelopeResponses.status(
                HttpStatus.CREATED,
                servletRequest,
                "Solicitação criada com sucesso",
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
    @Operation(summary = "Listar solicitações do estudante", description = "Retorna as solicitações do usuário autenticado, com filtro opcional por status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitações carregadas com sucesso"),
            @ApiResponse(responseCode = "400", description = "Status inválido"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto")
    })
    public ResponseEntity<ApiEnvelope<List<SolicitacaoDTO>>> listMyRequests(
            @RequestParam(required = false) @Parameter(description = "Filtro opcional por status da solicitação") String status,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitações do estudante carregadas com sucesso",
                solicitacaoService.listCurrentUserRequests(authentication.getName(), parseStatus(status))
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
    @Operation(summary = "Listar pendentes do doador", description = "Retorna as solicitações pendentes recebidas pelo doador autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitações pendentes carregadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto")
    })
    public ResponseEntity<ApiEnvelope<List<SolicitacaoDTO>>> listPendingDonorRequests(
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitações pendentes do doador carregadas com sucesso",
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
    @Operation(summary = "Listar aprovadas do doador", description = "Retorna as solicitações aprovadas ainda ativas para o doador autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitações aprovadas carregadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto")
    })
    public ResponseEntity<ApiEnvelope<List<SolicitacaoDTO>>> listApprovedDonorRequests(
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitações aprovadas do doador carregadas com sucesso",
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
    @Operation(summary = "Consultar solicitação", description = "Retorna os detalhes de uma solicitação acessível ao usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitação carregada com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto"),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada")
    })
    public ResponseEntity<ApiEnvelope<SolicitacaoDTO>> getRequest(
            @PathVariable @Parameter(description = "Identificador da solicitação") String id,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitação carregada com sucesso",
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
    @Operation(summary = "Aprovar solicitação", description = "Aprova uma solicitação pendente e reserva o material para o estudante escolhido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitação aprovada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Fluxo inválido para aprovação"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto"),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada"),
            @ApiResponse(responseCode = "409", description = "Corrida de aprovação ou estado concorrente")
    })
    public ResponseEntity<ApiEnvelope<SolicitacaoDTO>> approveRequest(
            @PathVariable @Parameter(description = "Identificador da solicitação") String id,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitação aprovada com sucesso",
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
    @Operation(summary = "Recusar solicitação", description = "Recusa uma solicitação pendente do material.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitação recusada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Fluxo inválido para recusa"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto"),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada")
    })
    public ResponseEntity<ApiEnvelope<SolicitacaoDTO>> declineRequest(
            @PathVariable @Parameter(description = "Identificador da solicitação") String id,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitação recusada com sucesso",
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
    @Operation(summary = "Cancelar solicitação", description = "Cancela uma solicitação do estudante ou do doador quando o fluxo permitir.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitação cancelada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Fluxo inválido para cancelamento"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto"),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada")
    })
    public ResponseEntity<ApiEnvelope<SolicitacaoDTO>> cancelRequest(
            @PathVariable @Parameter(description = "Identificador da solicitação") String id,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Solicitação cancelada com sucesso",
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
    @Operation(summary = "Concluir doação", description = "Conclui a doação aprovada e marca o material como doado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Doação concluída com sucesso"),
            @ApiResponse(responseCode = "400", description = "Fluxo inválido para conclusão"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto"),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada")
    })
    public ResponseEntity<ApiEnvelope<SolicitacaoDTO>> completeDonation(
            @PathVariable @Parameter(description = "Identificador da solicitação") String id,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
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
