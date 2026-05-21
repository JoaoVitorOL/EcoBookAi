package com.ecobook.service;

import com.ecobook.dto.PagedResponseDTO;
import com.ecobook.dto.ResolveNonReceiptReportRequestDTO;
import com.ecobook.exception.BadRequestException;
import com.ecobook.exception.ConflictException;
import com.ecobook.model.Material;
import com.ecobook.model.MaterialNonReceiptReport;
import com.ecobook.model.Solicitacao;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.NonReceiptReportStatus;
import com.ecobook.model.enums.Role;
import com.ecobook.repository.MaterialNonReceiptReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminReportServiceTest {

    private final MaterialNonReceiptReportRepository materialNonReceiptReportRepository =
            mock(MaterialNonReceiptReportRepository.class);

    private final AdminReportService adminReportService = new AdminReportService(materialNonReceiptReportRepository);

    @Test
    @DisplayName("listReports should return paged reports with related context")
    void shouldListReports() {
        MaterialNonReceiptReport report = sampleReport(NonReceiptReportStatus.OPEN);
        PageRequest pageRequest = PageRequest.of(0, 20);

        when(materialNonReceiptReportRepository.findByStatusOrderByCreatedAtDesc(NonReceiptReportStatus.OPEN, pageRequest))
                .thenReturn(new PageImpl<>(java.util.List.of(report), pageRequest, 1));

        PagedResponseDTO<?> response = adminReportService.listReports(NonReceiptReportStatus.OPEN, pageRequest);

        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().getFirst().toString()).contains("Colecao reportada");
    }

    @Test
    @DisplayName("resolveReport should resolve an open report and keep resolution notes")
    void shouldResolveReport() {
        MaterialNonReceiptReport report = sampleReport(NonReceiptReportStatus.OPEN);

        when(materialNonReceiptReportRepository.findDetailedById(report.getId())).thenReturn(Optional.of(report));
        when(materialNonReceiptReportRepository.save(any(MaterialNonReceiptReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = adminReportService.resolveReport(
                report.getId().toString(),
                new ResolveNonReceiptReportRequestDTO("Contato validado e caso encerrado")
        );

        assertThat(response.getStatus()).isEqualTo("RESOLVED");
        assertThat(response.getResolutionNotes()).isEqualTo("Contato validado e caso encerrado");
        assertThat(response.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("resolveReport should reject already resolved reports")
    void shouldRejectAlreadyResolvedReports() {
        MaterialNonReceiptReport report = sampleReport(NonReceiptReportStatus.RESOLVED);
        report.setResolvedAt(LocalDateTime.now().minusHours(1));

        when(materialNonReceiptReportRepository.findDetailedById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> adminReportService.resolveReport(report.getId().toString(), null))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Este reporte ja foi resolvido");
    }

    @Test
    @DisplayName("resolveReport should reject notes longer than 1000 characters")
    void shouldRejectTooLongResolutionNotes() {
        MaterialNonReceiptReport report = sampleReport(NonReceiptReportStatus.OPEN);
        String longNotes = "x".repeat(1001);

        when(materialNonReceiptReportRepository.findDetailedById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> adminReportService.resolveReport(
                report.getId().toString(),
                new ResolveNonReceiptReportRequestDTO(longNotes)
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("As notas de resolucao sao invalidas");
    }

    private MaterialNonReceiptReport sampleReport(NonReceiptReportStatus status) {
        Usuario donor = Usuario.builder()
                .id(UUID.randomUUID())
                .email("donor-admin@example.com")
                .passwordHash("hash")
                .nome("Doador Admin")
                .role(Role.USER)
                .build();

        Usuario student = Usuario.builder()
                .id(UUID.randomUUID())
                .email("student-admin@example.com")
                .passwordHash("hash")
                .nome("Estudante Admin")
                .role(Role.USER)
                .build();

        Material material = Material.builder()
                .id(UUID.randomUUID())
                .doador(donor)
                .titulo("Colecao reportada")
                .build();

        Solicitacao solicitacao = Solicitacao.builder()
                .id(UUID.randomUUID())
                .material(material)
                .estudante(student)
                .build();

        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);

        return MaterialNonReceiptReport.builder()
                .id(UUID.randomUUID())
                .material(material)
                .solicitacao(solicitacao)
                .estudante(student)
                .reason("Nao chegou")
                .status(status)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }
}
