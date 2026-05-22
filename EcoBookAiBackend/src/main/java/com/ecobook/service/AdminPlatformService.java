package com.ecobook.service;

import com.ecobook.dto.AdminUserSummaryDTO;
import com.ecobook.dto.MaterialDTO;
import com.ecobook.dto.PagedResponseDTO;
import com.ecobook.event.NotificationRequestedEvent;
import com.ecobook.exception.BadRequestException;
import com.ecobook.exception.ResourceNotFoundException;
import com.ecobook.model.Material;
import com.ecobook.model.Solicitacao;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.NonReceiptReportStatus;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.model.enums.StatusSolicitacao;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.SolicitacaoRepository;
import com.ecobook.repository.TemporaryUploadRepository;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.repository.projection.AdminUserMetricsProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPlatformService {

    private final MaterialRepository materialRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final TemporaryUploadRepository temporaryUploadRepository;
    private final MaterialMapper materialMapper;
    private final ImageStorageService imageStorageService;
    private final NotificationPayloadFactory notificationPayloadFactory;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PagedResponseDTO<MaterialDTO> listMaterials(StatusMaterial status, PageRequest pageRequest) {
        Page<Material> page = status == null
                ? materialRepository.findAllByOrderByCriadoEmDesc(pageRequest)
                : materialRepository.findByStatusOrderByCriadoEmDesc(status, pageRequest);

        return PagedResponseDTO.of(
                page.getContent().stream().map(materialMapper::toDto).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    @Transactional
    public void deleteMaterial(String materialId) {
        Material material = materialRepository.findById(parseMaterialId(materialId))
                .orElseThrow(() -> new ResourceNotFoundException("Material não encontrado"));

        List<Solicitacao> requests = solicitacaoRepository.findByMaterialId(material.getId());

        temporaryUploadRepository.findByMaterialId(material.getId()).ifPresent(upload -> {
            imageStorageService.deleteIfExists(upload.getFilePath());
            imageStorageService.deleteIfExists(upload.getSecondaryFilePath());
            temporaryUploadRepository.delete(upload);
        });

        materialRepository.delete(material);

        publishAdministrativeRemovalNotifications(material, requests);
    }

    @Transactional(readOnly = true)
    public PagedResponseDTO<AdminUserSummaryDTO> listUsers(PageRequest pageRequest) {
        Page<Usuario> page = usuarioRepository.findAllByOrderByCriadoEmDesc(pageRequest);
        List<UUID> userIds = page.getContent().stream()
                .map(Usuario::getId)
                .toList();

        Map<UUID, AdminUserMetricsProjection> metricsByUserId = userIds.isEmpty()
                ? Map.of()
                : usuarioRepository.findAdminMetricsByIds(
                                userIds,
                                StatusMaterial.DOADO,
                                StatusSolicitacao.CONCLUIDA,
                                NonReceiptReportStatus.OPEN
                        ).stream()
                        .collect(Collectors.toMap(AdminUserMetricsProjection::getUserId, Function.identity()));

        return PagedResponseDTO.of(
                page.getContent().stream()
                        .map(usuario -> toAdminUserSummary(usuario, metricsByUserId.get(usuario.getId())))
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    private void publishAdministrativeRemovalNotifications(Material material, List<Solicitacao> requests) {
        Usuario doador = material.getDoador();

        if (doador != null && doador.getId() != null) {
            eventPublisher.publishEvent(new NotificationRequestedEvent(
                    doador.getId(),
                    notificationPayloadFactory.materialRemovedByAdmin(
                            null,
                            material.getId().toString(),
                            material.getTitulo(),
                            doador,
                            null
                    )
            ));
        }

        requests.stream()
                .filter(request -> request.getEstudante() != null && request.getEstudante().getId() != null)
                .forEach(request -> eventPublisher.publishEvent(new NotificationRequestedEvent(
                        request.getEstudante().getId(),
                        notificationPayloadFactory.materialRemovedByAdmin(
                                request.getId().toString(),
                                material.getId().toString(),
                                material.getTitulo(),
                                doador,
                                request.getEstudante()
                        )
                )));
    }

    private AdminUserSummaryDTO toAdminUserSummary(Usuario usuario, AdminUserMetricsProjection metrics) {
        return AdminUserSummaryDTO.builder()
                .id(usuario.getId().toString())
                .email(usuario.getEmail())
                .nome(usuario.getNome())
                .whatsapp(usuario.getWhatsapp())
                .cidade(usuario.getCidade())
                .bairro(usuario.getBairro())
                .instituicao(usuario.getInstituicao())
                .perfilCompleto(usuario.getPerfilCompleto())
                .consentimentoIa(usuario.getConsentimentoIa())
                .role(usuario.getRole().name())
                .necessidadesAcademicas(usuario.getNecessidadesAcademicas() == null ? Set.of() :
                        usuario.getNecessidadesAcademicas().stream()
                                .map(Enum::name)
                                .collect(Collectors.toCollection(java.util.LinkedHashSet::new)))
                .materialsCount(metricValue(metrics == null ? null : metrics.getMaterialsCount()))
                .donatedMaterialsCount(metricValue(metrics == null ? null : metrics.getDonatedMaterialsCount()))
                .requestsCount(metricValue(metrics == null ? null : metrics.getRequestsCount()))
                .completedRequestsCount(metricValue(metrics == null ? null : metrics.getCompletedRequestsCount()))
                .openReportsCount(metricValue(metrics == null ? null : metrics.getOpenReportsCount()))
                .criadoEm(usuario.getCriadoEm())
                .atualizadoEm(usuario.getAtualizadoEm())
                .build();
    }

    private long metricValue(Long value) {
        return value == null ? 0L : value;
    }

    private UUID parseMaterialId(String materialId) {
        if (!StringUtils.hasText(materialId)) {
            throw new BadRequestException("Identificador de material inválido", Map.of(
                    "material_id", "Informe um UUID válido"
            ));
        }

        try {
            return UUID.fromString(materialId.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Identificador de material inválido", Map.of(
                    "material_id", "Informe um UUID válido"
            ));
        }
    }
}
