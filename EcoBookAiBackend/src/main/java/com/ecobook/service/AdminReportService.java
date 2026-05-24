package com.ecobook.service;

import com.ecobook.dto.AdminNonReceiptReportDTO;
import com.ecobook.dto.PagedResponseDTO;
import com.ecobook.dto.ResolveNonReceiptReportRequestDTO;
import com.ecobook.exception.BadRequestException;
import com.ecobook.exception.ConflictException;
import com.ecobook.exception.ResourceNotFoundException;
import com.ecobook.model.Material;
import com.ecobook.model.MaterialNonReceiptReport;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.NonReceiptReportStatus;
import com.ecobook.repository.MaterialNonReceiptReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final MaterialNonReceiptReportRepository materialNonReceiptReportRepository;

    /**
     * Lists paged non-receipt reports for administrative triage.
     * @param status optional status filter
     * @param pageRequest pagination settings for the query
     * @return requested list
     */
    @Transactional(readOnly = true)
    public PagedResponseDTO<AdminNonReceiptReportDTO> listReports(NonReceiptReportStatus status, PageRequest pageRequest) {
        Page<MaterialNonReceiptReport> page = status == null
                ? materialNonReceiptReportRepository.findAllByOrderByCreatedAtDesc(pageRequest)
                : materialNonReceiptReportRepository.findByStatusOrderByCreatedAtDesc(status, pageRequest);

        return PagedResponseDTO.of(
                page.getContent().stream().map(this::toDto).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    /**
     * Resolves a non-receipt report and stores the moderation notes.
     * @param reportId r ep or ti d
     * @param request request payload for the operation
     * @return result of the operation
     */
    @Transactional
    public AdminNonReceiptReportDTO resolveReport(String reportId, ResolveNonReceiptReportRequestDTO request) {
        MaterialNonReceiptReport report = materialNonReceiptReportRepository.findDetailedById(parseReportId(reportId))
                .orElseThrow(() -> new ResourceNotFoundException("Reporte não encontrado"));

        if (report.getStatus() == NonReceiptReportStatus.RESOLVED) {
            throw new ConflictException("Este reporte ja foi resolvido");
        }

        String resolutionNotes = trimToNull(request != null ? request.getResolutionNotes() : null);
        if (resolutionNotes != null && resolutionNotes.length() > 1000) {
            throw new BadRequestException(
                    "As notas de resolucao sao invalidas",
                    Map.of("resolution_notes", "As notas de resolução devem ter no máximo 1000 caracteres")
            );
        }

        LocalDateTime now = LocalDateTime.now();
        report.setStatus(NonReceiptReportStatus.RESOLVED);
        report.setResolvedAt(now);
        report.setUpdatedAt(now);
        report.setResolutionNotes(resolutionNotes);

        return toDto(materialNonReceiptReportRepository.save(report));
    }

    private UUID parseReportId(String reportId) {
        if (!StringUtils.hasText(reportId)) {
            throw new BadRequestException("Identificador de reporte invalido", Map.of(
                    "report_id", "Informe um UUID válido"
            ));
        }

        try {
            return UUID.fromString(reportId.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Identificador de reporte invalido", Map.of(
                    "report_id", "Informe um UUID válido"
            ));
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private AdminNonReceiptReportDTO toDto(MaterialNonReceiptReport report) {
        Material material = report.getMaterial();
        Usuario estudante = report.getEstudante();
        Usuario doador = material == null ? null : material.getDoador();

        return AdminNonReceiptReportDTO.builder()
                .id(report.getId().toString())
                .materialId(material == null || material.getId() == null ? null : material.getId().toString())
                .materialTitulo(material == null ? null : material.getTitulo())
                .solicitacaoId(report.getSolicitacao() == null || report.getSolicitacao().getId() == null
                        ? null
                        : report.getSolicitacao().getId().toString())
                .estudanteId(estudante == null || estudante.getId() == null ? null : estudante.getId().toString())
                .estudanteNome(estudante == null ? null : estudante.getNome())
                .estudanteEmail(estudante == null ? null : estudante.getEmail())
                .doadorId(doador == null || doador.getId() == null ? null : doador.getId().toString())
                .doadorNome(doador == null ? null : doador.getNome())
                .reason(report.getReason())
                .status(report.getStatus().name())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .resolvedAt(report.getResolvedAt())
                .resolutionNotes(report.getResolutionNotes())
                .build();
    }
}
