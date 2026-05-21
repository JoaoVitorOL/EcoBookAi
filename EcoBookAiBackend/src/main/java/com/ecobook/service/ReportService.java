package com.ecobook.service;

import com.ecobook.dto.CreateNonReceiptReportRequestDTO;
import com.ecobook.dto.MaterialNonReceiptReportDTO;
import com.ecobook.event.NonReceiptReportCreatedEvent;
import com.ecobook.exception.BadRequestException;
import com.ecobook.exception.ConflictException;
import com.ecobook.exception.ResourceNotFoundException;
import com.ecobook.exception.UnprocessableEntityException;
import com.ecobook.model.Material;
import com.ecobook.model.MaterialNonReceiptReport;
import com.ecobook.model.Solicitacao;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.NonReceiptReportStatus;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.model.enums.StatusSolicitacao;
import com.ecobook.repository.MaterialNonReceiptReportRepository;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.SolicitacaoRepository;
import com.ecobook.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UsuarioRepository usuarioRepository;
    private final MaterialRepository materialRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final MaterialNonReceiptReportRepository materialNonReceiptReportRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public MaterialNonReceiptReportDTO reportNonReceipt(String email,
                                                        String materialId,
                                                        CreateNonReceiptReportRequestDTO request) {
        Usuario estudante = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        Material material = materialRepository.findById(parseMaterialId(materialId))
                .orElseThrow(() -> new ResourceNotFoundException("Material nao encontrado"));

        if (material.getStatus() != StatusMaterial.DOADO) {
            throw new UnprocessableEntityException("Somente materiais marcados como doados podem ser reportados");
        }

        Solicitacao solicitacao = solicitacaoRepository
                .findFirstByMaterialIdAndEstudanteIdAndStatusOrderByCriadoEmDesc(
                        material.getId(),
                        estudante.getId(),
                        StatusSolicitacao.CONCLUIDA
                )
                .orElseThrow(() -> new AccessDeniedException(
                        "Apenas o estudante com solicitacao concluida pode reportar nao recebimento"
                ));

        if (materialNonReceiptReportRepository.existsBySolicitacaoIdAndStatus(
                solicitacao.getId(),
                NonReceiptReportStatus.OPEN
        )) {
            throw new ConflictException("Ja existe um reporte aberto para este material");
        }

        String reason = trimToNull(request != null ? request.getReason() : null);
        if (reason != null && reason.length() > 500) {
            throw new BadRequestException(
                    "O motivo informado e invalido",
                    Map.of("reason", "O motivo deve ter no maximo 500 caracteres")
            );
        }

        MaterialNonReceiptReport report = materialNonReceiptReportRepository.save(MaterialNonReceiptReport.builder()
                .material(material)
                .solicitacao(solicitacao)
                .estudante(estudante)
                .reason(reason)
                .status(NonReceiptReportStatus.OPEN)
                .build());

        eventPublisher.publishEvent(new NonReceiptReportCreatedEvent(
                report.getId().toString(),
                material.getId().toString(),
                solicitacao.getId().toString(),
                estudante.getId().toString()
        ));

        return toDto(report);
    }

    private UUID parseMaterialId(String materialId) {
        if (!StringUtils.hasText(materialId)) {
            throw new BadRequestException("Identificador de material invalido", Map.of(
                    "material_id", "Informe um UUID valido"
            ));
        }

        try {
            return UUID.fromString(materialId.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Identificador de material invalido", Map.of(
                    "material_id", "Informe um UUID valido"
            ));
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private MaterialNonReceiptReportDTO toDto(MaterialNonReceiptReport report) {
        return MaterialNonReceiptReportDTO.builder()
                .id(report.getId().toString())
                .materialId(report.getMaterial().getId().toString())
                .solicitacaoId(report.getSolicitacao().getId().toString())
                .estudanteId(report.getEstudante().getId().toString())
                .reason(report.getReason())
                .status(report.getStatus().name())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }
}
