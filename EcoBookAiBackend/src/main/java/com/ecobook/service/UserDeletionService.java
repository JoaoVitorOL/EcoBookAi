package com.ecobook.service;

import com.ecobook.dto.DeleteAccountRequestDTO;
import com.ecobook.dto.DeleteAccountResponseDTO;
import com.ecobook.event.NotificationRequestedEvent;
import com.ecobook.exception.ResourceNotFoundException;
import com.ecobook.model.Material;
import com.ecobook.model.Solicitacao;
import com.ecobook.model.TemporaryUpload;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.model.enums.StatusSolicitacao;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.SolicitacaoRepository;
import com.ecobook.repository.TemporaryUploadRepository;
import com.ecobook.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserDeletionService {

    private final UsuarioRepository usuarioRepository;
    private final MaterialRepository materialRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final TemporaryUploadRepository temporaryUploadRepository;
    private final NotificationPayloadFactory notificationPayloadFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final ImageStorageService imageStorageService;
    private final PasswordEncoder passwordEncoder;
    private final ConsentService consentService;
    private final TokenRevocationService tokenRevocationService;
    private final AuditLogService auditLogService;

    @Transactional
    public DeleteAccountResponseDTO deleteCurrentUser(String email,
                                                      String rawToken,
                                                      DeleteAccountRequestDTO request) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (!StringUtils.hasText(usuario.getPasswordHash()) ||
                !passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new BadCredentialsException("Senha inválida");
        }

        UUID userId = usuario.getId();
        String originalEmail = usuario.getEmail();
        LocalDateTime now = LocalDateTime.now();

        List<Material> ownMaterials = materialRepository.findByDoadorIdOrderByCriadoEmDesc(userId);
        List<UUID> ownMaterialIds = ownMaterials.stream().map(Material::getId).toList();
        LinkedHashMap<UUID, Solicitacao> requestsToDelete = new LinkedHashMap<>();
        solicitacaoRepository.findByEstudanteIdOrderByCriadoEmDesc(userId)
                .forEach(requestItem -> requestsToDelete.put(requestItem.getId(), requestItem));
        solicitacaoRepository.findByMaterialDoadorIdOrderByCriadoEmDesc(userId)
                .forEach(requestItem -> requestsToDelete.put(requestItem.getId(), requestItem));

        publishDeletionNotifications(usuario, ownMaterials, requestsToDelete.values());
        cleanupTrackedUploads(userId, ownMaterialIds);

        ownMaterials.forEach(material -> {
            material.setStatus(StatusMaterial.CANCELADO);
            material.setImagemUrl(null);
            material.setImagemVersoUrl(null);
            material.setUploadId(null);
            material.setUploadTrackingId(null);
            material.markDeleted(userId, true);
        });
        if (!ownMaterials.isEmpty()) {
            materialRepository.saveAll(ownMaterials);
        }

        requestsToDelete.values().forEach(requestItem -> {
            if (requestItem.getStatus() == StatusSolicitacao.PENDENTE || requestItem.getStatus() == StatusSolicitacao.APROVADA) {
                requestItem.setStatus(StatusSolicitacao.CANCELADA);
            }
            requestItem.setContatoDoador(null);
            requestItem.markDeleted(userId, true);
        });
        if (!requestsToDelete.isEmpty()) {
            solicitacaoRepository.saveAll(requestsToDelete.values());
        }

        if (Boolean.TRUE.equals(usuario.getConsentimentoIa())) {
            consentService.recordAiConsentChange(usuario, false);
        }
        consentService.recordPlatformRevocation(usuario);

        usuario.setEmail("deleted+" + userId + "@deleted.ecobook.local");
        usuario.setPasswordHash(passwordEncoder.encode("deleted-" + UUID.randomUUID()));
        usuario.setNome("Conta removida");
        usuario.setWhatsapp(null);
        usuario.setCidade(null);
        usuario.setBairro(null);
        usuario.setInstituicao(null);
        usuario.setFcmToken(null);
        usuario.setPerfilCompleto(false);
        usuario.setConsentimentoIa(false);
        usuario.setNecessidadesAcademicas(new LinkedHashSet<>());
        usuario.markDeleted(userId, true);
        usuarioRepository.save(usuario);

        tokenRevocationService.revoke(rawToken, userId);
        deleteUserDirectory(userId);

        auditLogService.log(
                "ACCOUNT_DELETED",
                userId,
                originalEmail,
                userId,
                "USER",
                userId.toString(),
                buildDeletionDetails(request, ownMaterials.size(), requestsToDelete.size(), now)
        );

        return DeleteAccountResponseDTO.builder()
                .userId(userId.toString())
                .deletedAt(now)
                .build();
    }

    private void cleanupTrackedUploads(UUID userId, List<UUID> ownMaterialIds) {
        Map<UUID, TemporaryUpload> uploadsById = new LinkedHashMap<>();
        temporaryUploadRepository.findByUsuarioId(userId)
                .forEach(upload -> uploadsById.put(upload.getId(), upload));
        if (!ownMaterialIds.isEmpty()) {
            temporaryUploadRepository.findByMaterialIdIn(ownMaterialIds)
                    .forEach(upload -> uploadsById.put(upload.getId(), upload));
        }

        List<TemporaryUpload> uploads = new ArrayList<>(uploadsById.values());
        uploads.forEach(upload -> {
            imageStorageService.deleteIfExists(upload.getFilePath());
            imageStorageService.deleteIfExists(upload.getSecondaryFilePath());
        });
        if (!uploads.isEmpty()) {
            temporaryUploadRepository.deleteAll(uploads);
        }
    }

    private void publishDeletionNotifications(Usuario deletedUser,
                                              List<Material> ownMaterials,
                                              java.util.Collection<Solicitacao> affectedRequests) {
        for (Material material : ownMaterials) {
            List<Solicitacao> requests = solicitacaoRepository.findByMaterialId(material.getId());
            for (Solicitacao request : requests) {
                if (request.getEstudante() == null || request.getEstudante().getId() == null) {
                    continue;
                }
                eventPublisher.publishEvent(new NotificationRequestedEvent(
                        request.getEstudante().getId(),
                        notificationPayloadFactory.materialCanceled(
                                request.getId().toString(),
                                material.getId().toString(),
                                material.getTitulo(),
                                material.getDoador(),
                                request.getEstudante()
                        )
                ));
            }
        }

        for (Solicitacao request : affectedRequests) {
            if (request.getMaterial() == null || request.getMaterial().getDoador() == null || request.getMaterial().getDoador().getId() == null) {
                continue;
            }
            if (request.getEstudante() == null || !request.getEstudante().getId().equals(deletedUser.getId())) {
                continue;
            }
            eventPublisher.publishEvent(new NotificationRequestedEvent(
                    request.getMaterial().getDoador().getId(),
                    notificationPayloadFactory.requestCanceledByStudent(
                            request.getId().toString(),
                            request.getMaterial().getId().toString(),
                            request.getMaterial().getTitulo(),
                            request.getMaterial().getDoador(),
                            request.getEstudante()
                    )
            ));
        }
    }

    private Map<String, String> buildDeletionDetails(DeleteAccountRequestDTO request,
                                                     int materialsCount,
                                                     int requestsCount,
                                                     LocalDateTime deletedAt) {
        LinkedHashMap<String, String> details = new LinkedHashMap<>();
        details.put("deleted_at", deletedAt.toString());
        details.put("materials_removed", Integer.toString(materialsCount));
        details.put("requests_removed", Integer.toString(requestsCount));
        if (StringUtils.hasText(request.getReason())) {
            details.put("reason", request.getReason().trim());
        }
        return details;
    }

    private void deleteUserDirectory(UUID userId) {
        Path userDirectory = Path.of(imageStorageService.getUploadDir(), userId.toString()).toAbsolutePath().normalize();
        if (!Files.exists(userDirectory)) {
            return;
        }

        try (java.util.stream.Stream<Path> stream = Files.walk(userDirectory)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Individual file cleanup failures are already tolerated elsewhere.
                        }
                    });
        } catch (IOException ignored) {
            // Directory cleanup is best-effort to avoid blocking account deletion.
        }
    }
}
