package com.ecobook.service;

import com.ecobook.dto.SolicitacaoDTO;
import com.ecobook.event.NotificationRequestedEvent;
import com.ecobook.exception.BadRequestException;
import com.ecobook.exception.ConflictException;
import com.ecobook.exception.ResourceNotFoundException;
import com.ecobook.exception.UnprocessableEntityException;
import com.ecobook.model.Material;
import com.ecobook.model.Solicitacao;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.model.enums.StatusSolicitacao;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.SolicitacaoRepository;
import com.ecobook.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Solicitacao service for request/solicitation lifecycle.
 */
@Service
@RequiredArgsConstructor
public class SolicitacaoService {

    private final UsuarioRepository usuarioRepository;
    private final MaterialRepository materialRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final SolicitacaoMapper solicitacaoMapper;
    private final MaterialStateValidator materialStateValidator;
    private final NotificationPayloadFactory notificationPayloadFactory;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Creates a new request for a material on behalf of the authenticated student.
     * @param email authenticated user email
     * @param materialId material identifier
     * @return created result
     */
    @Transactional
    public SolicitacaoDTO createRequest(String email, String materialId) {
        Usuario estudante = loadUsuario(email);
        Material material = loadMaterial(materialId);

        if (material.getDoador().getId().equals(estudante.getId())) {
            throw new BadRequestException("Não é possível solicitar o próprio material", Map.of());
        }

        if (material.getStatus() == StatusMaterial.RESERVADO) {
            throw new ConflictException("O material já possui uma solicitação aprovada");
        }

        if (material.getStatus() != StatusMaterial.DISPONIVEL) {
            throw new UnprocessableEntityException("O material não está disponível para novas solicitações");
        }

        if (solicitacaoRepository.existsByMaterialIdAndEstudanteIdAndStatusIn(
                material.getId(),
                estudante.getId(),
                List.of(StatusSolicitacao.PENDENTE, StatusSolicitacao.APROVADA)
        )) {
            throw new ConflictException("Você já possui uma solicitação ativa para este material");
        }

        Solicitacao solicitacao = solicitacaoRepository.save(Solicitacao.builder()
                .material(material)
                .estudante(estudante)
                .status(StatusSolicitacao.PENDENTE)
                .build());

        eventPublisher.publishEvent(new NotificationRequestedEvent(
                material.getDoador().getId(),
                notificationPayloadFactory.requestReceived(
                        solicitacao.getId().toString(),
                        material.getId().toString(),
                        material.getTitulo(),
                        estudante
                )
        ));

        return solicitacaoMapper.toDto(solicitacao);
    }

    /**
     * Lists the current user's requests, optionally filtered by status.
     * @param email authenticated user email
     * @param status optional status filter
     * @return requested list
     */
    @Transactional(readOnly = true)
    public List<SolicitacaoDTO> listCurrentUserRequests(String email, StatusSolicitacao status) {
        Usuario usuario = loadUsuario(email);
        return solicitacaoRepository.findByEstudanteIdOrderByCriadoEmDesc(usuario.getId()).stream()
                .filter(request -> status == null || request.getStatus() == status)
                .map(solicitacaoMapper::toDto)
                .toList();
    }

    /**
     * Lists pending requests for materials owned by the authenticated donor.
     * @param email authenticated user email
     * @return requested list
     */
    @Transactional(readOnly = true)
    public List<SolicitacaoDTO> listPendingRequestsForDonor(String email) {
        Usuario usuario = loadUsuario(email);
        return solicitacaoRepository
                .findByMaterialDoadorIdAndStatusOrderByCriadoEmDesc(usuario.getId(), StatusSolicitacao.PENDENTE)
                .stream()
                .map(solicitacaoMapper::toDto)
                .toList();
    }

    /**
     * Lists approved requests for materials owned by the authenticated donor.
     * @param email authenticated user email
     * @return requested list
     */
    @Transactional(readOnly = true)
    public List<SolicitacaoDTO> listApprovedRequestsForDonor(String email) {
        Usuario usuario = loadUsuario(email);
        return solicitacaoRepository
                .findByMaterialDoadorIdAndStatusOrderByCriadoEmDesc(usuario.getId(), StatusSolicitacao.APROVADA)
                .stream()
                .map(solicitacaoMapper::toDto)
                .toList();
    }

    /**
     * Loads a request visible to one of its participants.
     * @param email authenticated user email
     * @param requestId request identifier
     * @return requested value
     */
    @Transactional(readOnly = true)
    public SolicitacaoDTO getRequest(String email, String requestId) {
        Usuario usuario = loadUsuario(email);
        Solicitacao solicitacao = loadSolicitacao(requestId);
        ensureParticipant(solicitacao, usuario);
        return solicitacaoMapper.toDto(solicitacao);
    }

    /**
     * Approves a pending request and reserves the related material.
     * @param email authenticated user email
     * @param requestId request identifier
     * @return result of the operation
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SolicitacaoDTO approveRequest(String email, String requestId) {
        Usuario doador = loadUsuario(email);
        Solicitacao solicitacao = loadSolicitacaoForUpdate(requestId);
        Material material = lockMaterial(solicitacao.getMaterial().getId());

        ensureDonor(solicitacao, doador);
        ensureRequestStatus(solicitacao, StatusSolicitacao.PENDENTE, "Somente solicitacoes pendentes podem ser aprovadas");
        materialStateValidator.requireAvailable(
                material.getStatus(),
                "O material precisa estar DISPONIVEL para aprovar uma solicitacao"
        );

        if (solicitacaoRepository.existsByMaterialIdAndStatusAndIdNot(
                material.getId(),
                StatusSolicitacao.APROVADA,
                solicitacao.getId()
        )) {
            throw new ConflictException("Ja existe outra solicitacao aprovada para este material");
        }

        LocalDateTime now = LocalDateTime.now();
        materialStateValidator.validateTransition(material.getStatus(), StatusMaterial.RESERVADO);
        material.setStatus(StatusMaterial.RESERVADO);
        material.setDoadoEm(null);

        solicitacao.setStatus(StatusSolicitacao.APROVADA);
        solicitacao.setAprovadoEm(now);
        solicitacao.setExpiresAt(now.plusDays(14));
        solicitacao.populateContatoDoador(doador);

        List<Solicitacao> otherPending = solicitacaoRepository.findByMaterialIdAndStatus(material.getId(), StatusSolicitacao.PENDENTE)
                .stream()
                .filter(other -> !other.getId().equals(solicitacao.getId()))
                .peek(other -> other.setStatus(StatusSolicitacao.RECUSADA))
                .toList();

        materialRepository.save(material);
        solicitacaoRepository.save(solicitacao);
        if (!otherPending.isEmpty()) {
            solicitacaoRepository.saveAll(otherPending);
        }

        notifyApproval(solicitacao);
        otherPending.forEach(this::notifyDecline);
        return solicitacaoMapper.toDto(solicitacao);
    }

    /**
     * Declines a pending request for a donor-owned material.
     * @param email authenticated user email
     * @param requestId request identifier
     * @return result of the operation
     */
    @Transactional
    public SolicitacaoDTO declineRequest(String email, String requestId) {
        Usuario doador = loadUsuario(email);
        Solicitacao solicitacao = loadSolicitacao(requestId);
        ensureDonor(solicitacao, doador);
        ensureRequestStatus(solicitacao, StatusSolicitacao.PENDENTE, "Somente solicitacoes pendentes podem ser recusadas");

        solicitacao.setStatus(StatusSolicitacao.RECUSADA);
        clearApprovalContext(solicitacao, false);
        solicitacao.setConcluidoEm(null);
        Solicitacao saved = solicitacaoRepository.save(solicitacao);
        notifyDecline(saved);
        return solicitacaoMapper.toDto(saved);
    }

    /**
     * Cancels a pending or approved request and restores availability when needed.
     * @param email authenticated user email
     * @param requestId request identifier
     * @return result of the operation
     */
    @Transactional
    public SolicitacaoDTO cancelRequest(String email, String requestId) {
        Usuario actor = loadUsuario(email);
        Solicitacao solicitacao = loadSolicitacao(requestId);
        ensureParticipant(solicitacao, actor);

        if (!EnumSet.of(StatusSolicitacao.PENDENTE, StatusSolicitacao.APROVADA).contains(solicitacao.getStatus())) {
            throw new UnprocessableEntityException("Somente solicitacoes pendentes ou aprovadas podem ser canceladas");
        }

        if (solicitacao.getStatus() == StatusSolicitacao.APROVADA) {
            Material material = lockMaterial(solicitacao.getMaterial().getId());
            materialStateValidator.validateTransition(material.getStatus(), StatusMaterial.DISPONIVEL);
            material.setStatus(StatusMaterial.DISPONIVEL);
            material.setDoadoEm(null);
            materialRepository.save(material);
        }

        solicitacao.setStatus(StatusSolicitacao.CANCELADA);
        clearApprovalContext(solicitacao, true);
        solicitacao.setConcluidoEm(null);
        Solicitacao saved = solicitacaoRepository.save(solicitacao);
        notifyCancellation(saved, actor);
        return solicitacaoMapper.toDto(saved);
    }

    /**
     * Completes a donation for an approved request.
     * @param email authenticated user email
     * @param requestId request identifier
     * @return result of the operation
     */
    @Transactional
    public SolicitacaoDTO completeDonation(String email, String requestId) {
        Usuario doador = loadUsuario(email);
        Solicitacao solicitacao = loadSolicitacao(requestId);
        Material material = lockMaterial(solicitacao.getMaterial().getId());

        ensureDonor(solicitacao, doador);
        ensureRequestStatus(solicitacao, StatusSolicitacao.APROVADA, "Somente solicitacoes aprovadas podem ser concluidas");
        materialStateValidator.validateTransition(material.getStatus(), StatusMaterial.DOADO);

        LocalDateTime now = LocalDateTime.now();
        solicitacao.setStatus(StatusSolicitacao.CONCLUIDA);
        solicitacao.setConcluidoEm(now);
        clearApprovalContext(solicitacao, true);
        material.setStatus(StatusMaterial.DOADO);
        material.setDoadoEm(now);

        materialRepository.save(material);
        Solicitacao saved = solicitacaoRepository.save(solicitacao);
        notifyCompletion(saved);
        return solicitacaoMapper.toDto(saved);
    }

    /**
     * Expires approved requests that passed their reservation deadline.
     * @return result of the operation
     */
    @Transactional
    public int expireApprovedRequests() {
        LocalDateTime now = LocalDateTime.now();
        List<Solicitacao> expiredRequests = solicitacaoRepository.findByStatusAndExpiresAtIsNotNull(StatusSolicitacao.APROVADA)
                .stream()
                .filter(request -> request.hasExpired(now))
                .toList();

        expiredRequests.forEach(request -> {
            Material material = lockMaterial(request.getMaterial().getId());
            if (material.getStatus() == StatusMaterial.RESERVADO) {
                material.setStatus(StatusMaterial.DISPONIVEL);
                material.setDoadoEm(null);
                materialRepository.save(material);
            }
            request.setStatus(StatusSolicitacao.CANCELADA);
            clearApprovalContext(request, true);
            request.setConcluidoEm(null);
            solicitacaoRepository.save(request);
            eventPublisher.publishEvent(new NotificationRequestedEvent(
                    request.getEstudante().getId(),
                    notificationPayloadFactory.requestExpired(
                            request.getId().toString(),
                            material.getId().toString(),
                            material.getTitulo(),
                            material.getDoador(),
                            request.getEstudante()
                    )
            ));
        });

        return expiredRequests.size();
    }

    private Usuario loadUsuario(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private Material loadMaterial(String materialId) {
        return materialRepository.findById(parseUuid(materialId, "material"))
                .orElseThrow(() -> new ResourceNotFoundException("Material não encontrado"));
    }

    private Material lockMaterial(UUID materialId) {
        return materialRepository.findByIdForUpdate(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material não encontrado"));
    }

    private Solicitacao loadSolicitacao(String requestId) {
        return solicitacaoRepository.findById(parseUuid(requestId, "solicitacao"))
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));
    }

    private Solicitacao loadSolicitacaoForUpdate(String requestId) {
        return solicitacaoRepository.findByIdForUpdate(parseUuid(requestId, "solicitacao"))
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));
    }

    private void ensureDonor(Solicitacao solicitacao, Usuario usuario) {
        if (!solicitacao.getMaterial().getDoador().getId().equals(usuario.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Apenas o doador do material pode alterar esta solicitacao"
            );
        }
    }

    private void ensureParticipant(Solicitacao solicitacao, Usuario usuario) {
        boolean isStudent = solicitacao.getEstudante().getId().equals(usuario.getId());
        boolean isDonor = solicitacao.getMaterial().getDoador().getId().equals(usuario.getId());
        if (!isStudent && !isDonor) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Apenas o estudante solicitante ou o doador podem acessar esta solicitacao"
            );
        }
    }

    private void ensureRequestStatus(Solicitacao solicitacao, StatusSolicitacao expected, String message) {
        if (solicitacao.getStatus() != expected) {
            throw new UnprocessableEntityException(message);
        }
    }

    private void clearApprovalContext(Solicitacao solicitacao, boolean keepApprovedTimestamp) {
        solicitacao.setContatoDoador(null);
        solicitacao.setExpiresAt(null);
        if (!keepApprovedTimestamp) {
            solicitacao.setAprovadoEm(null);
        }
    }

    private UUID parseUuid(String rawValue, String label) {
        if (!StringUtils.hasText(rawValue)) {
            throw new BadRequestException("Identificador de " + label + " inválido", Map.of(label, "Informe um UUID válido"));
        }

        try {
            return UUID.fromString(rawValue.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Identificador de " + label + " inválido", Map.of(label, "Informe um UUID válido"));
        }
    }

    private void notifyApproval(Solicitacao solicitacao) {
        eventPublisher.publishEvent(new NotificationRequestedEvent(
                solicitacao.getEstudante().getId(),
                notificationPayloadFactory.requestApproved(
                        solicitacao.getId().toString(),
                        solicitacao.getMaterial().getId().toString(),
                        solicitacao.getMaterial().getTitulo(),
                        solicitacao.getMaterial().getDoador(),
                        solicitacao.getEstudante()
                )
        ));
    }

    private void notifyDecline(Solicitacao solicitacao) {
        eventPublisher.publishEvent(new NotificationRequestedEvent(
                solicitacao.getEstudante().getId(),
                notificationPayloadFactory.requestDeclined(
                        solicitacao.getId().toString(),
                        solicitacao.getMaterial().getId().toString(),
                        solicitacao.getMaterial().getTitulo(),
                        solicitacao.getMaterial().getDoador(),
                        solicitacao.getEstudante()
                )
        ));
    }

    private void notifyCancellation(Solicitacao solicitacao, Usuario actor) {
        Usuario recipient = solicitacao.getEstudante().getId().equals(actor.getId())
                ? solicitacao.getMaterial().getDoador()
                : solicitacao.getEstudante();
        boolean canceledByStudent = solicitacao.getEstudante().getId().equals(actor.getId());

        eventPublisher.publishEvent(new NotificationRequestedEvent(
                recipient.getId(),
                canceledByStudent
                        ? notificationPayloadFactory.requestCanceledByStudent(
                        solicitacao.getId().toString(),
                        solicitacao.getMaterial().getId().toString(),
                        solicitacao.getMaterial().getTitulo(),
                        solicitacao.getMaterial().getDoador(),
                        solicitacao.getEstudante()
                )
                        : notificationPayloadFactory.requestCanceledByDonor(
                        solicitacao.getId().toString(),
                        solicitacao.getMaterial().getId().toString(),
                        solicitacao.getMaterial().getTitulo(),
                        solicitacao.getMaterial().getDoador(),
                        solicitacao.getEstudante()
                )
        ));
    }

    private void notifyCompletion(Solicitacao solicitacao) {
        eventPublisher.publishEvent(new NotificationRequestedEvent(
                solicitacao.getEstudante().getId(),
                notificationPayloadFactory.donationCompleted(
                        solicitacao.getId().toString(),
                        solicitacao.getMaterial().getId().toString(),
                        solicitacao.getMaterial().getTitulo(),
                        solicitacao.getMaterial().getDoador(),
                        solicitacao.getEstudante()
                )
        ));
    }
}
