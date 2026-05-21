package com.ecobook.service;

import com.ecobook.dto.CreateNonReceiptReportRequestDTO;
import com.ecobook.event.NonReceiptReportCreatedEvent;
import com.ecobook.exception.ConflictException;
import com.ecobook.exception.UnprocessableEntityException;
import com.ecobook.model.Material;
import com.ecobook.model.MaterialNonReceiptReport;
import com.ecobook.model.Solicitacao;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Disciplina;
import com.ecobook.model.enums.EstadoConservacao;
import com.ecobook.model.enums.NivelEnsino;
import com.ecobook.model.enums.NonReceiptReportStatus;
import com.ecobook.model.enums.Role;
import com.ecobook.model.enums.SistemaEnsino;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.model.enums.StatusSolicitacao;
import com.ecobook.repository.MaterialNonReceiptReportRepository;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.SolicitacaoRepository;
import com.ecobook.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final MaterialRepository materialRepository = mock(MaterialRepository.class);
    private final SolicitacaoRepository solicitacaoRepository = mock(SolicitacaoRepository.class);
    private final MaterialNonReceiptReportRepository materialNonReceiptReportRepository = mock(MaterialNonReceiptReportRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private final ReportService reportService = new ReportService(
            usuarioRepository,
            materialRepository,
            solicitacaoRepository,
            materialNonReceiptReportRepository,
            eventPublisher
    );

    @Test
    @DisplayName("reportNonReceipt should create an OPEN report and publish an event")
    void shouldCreateOpenReport() {
        Usuario student = createStudent("student-report-service@example.com");
        Material material = createMaterial(StatusMaterial.DOADO);
        Solicitacao request = createCompletedRequest(material, student);

        when(usuarioRepository.findByEmailIgnoreCase(student.getEmail())).thenReturn(Optional.of(student));
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(solicitacaoRepository.findFirstByMaterialIdAndEstudanteIdAndStatusOrderByCriadoEmDesc(
                material.getId(),
                student.getId(),
                StatusSolicitacao.CONCLUIDA
        )).thenReturn(Optional.of(request));
        when(materialNonReceiptReportRepository.existsBySolicitacaoIdAndStatus(request.getId(), NonReceiptReportStatus.OPEN))
                .thenReturn(false);
        when(materialNonReceiptReportRepository.save(any(MaterialNonReceiptReport.class)))
                .thenAnswer(invocation -> {
                    MaterialNonReceiptReport report = invocation.getArgument(0);
                    report.setId(UUID.randomUUID());
                    report.setCreatedAt(LocalDateTime.now());
                    report.setUpdatedAt(report.getCreatedAt());
                    return report;
                });

        var response = reportService.reportNonReceipt(
                student.getEmail(),
                material.getId().toString(),
                new CreateNonReceiptReportRequestDTO("O material nao foi entregue")
        );

        assertThat(response.getStatus()).isEqualTo("OPEN");
        assertThat(response.getMaterialId()).isEqualTo(material.getId().toString());
        assertThat(response.getSolicitacaoId()).isEqualTo(request.getId().toString());
        assertThat(response.getEstudanteId()).isEqualTo(student.getId().toString());
        assertThat(response.getReason()).isEqualTo("O material nao foi entregue");

        ArgumentCaptor<NonReceiptReportCreatedEvent> captor = ArgumentCaptor.forClass(NonReceiptReportCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().materialId()).isEqualTo(material.getId().toString());
        assertThat(captor.getValue().solicitacaoId()).isEqualTo(request.getId().toString());
        assertThat(captor.getValue().estudanteId()).isEqualTo(student.getId().toString());
    }

    @Test
    @DisplayName("reportNonReceipt should reject materials that are not marked as donated")
    void shouldRejectNonDonatedMaterial() {
        Usuario student = createStudent("student-report-service-pending@example.com");
        Material material = createMaterial(StatusMaterial.DISPONIVEL);

        when(usuarioRepository.findByEmailIgnoreCase(student.getEmail())).thenReturn(Optional.of(student));
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> reportService.reportNonReceipt(
                student.getEmail(),
                material.getId().toString(),
                new CreateNonReceiptReportRequestDTO("Ainda nao recebi")
        ))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage("Somente materiais marcados como doados podem ser reportados");
    }

    @Test
    @DisplayName("reportNonReceipt should reject duplicate open reports")
    void shouldRejectDuplicateOpenReports() {
        Usuario student = createStudent("student-report-service-duplicate@example.com");
        Material material = createMaterial(StatusMaterial.DOADO);
        Solicitacao request = createCompletedRequest(material, student);

        when(usuarioRepository.findByEmailIgnoreCase(student.getEmail())).thenReturn(Optional.of(student));
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(solicitacaoRepository.findFirstByMaterialIdAndEstudanteIdAndStatusOrderByCriadoEmDesc(
                material.getId(),
                student.getId(),
                StatusSolicitacao.CONCLUIDA
        )).thenReturn(Optional.of(request));
        when(materialNonReceiptReportRepository.existsBySolicitacaoIdAndStatus(request.getId(), NonReceiptReportStatus.OPEN))
                .thenReturn(true);

        assertThatThrownBy(() -> reportService.reportNonReceipt(
                student.getEmail(),
                material.getId().toString(),
                new CreateNonReceiptReportRequestDTO("Tentativa repetida")
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ja existe um reporte aberto para este material");
    }

    @Test
    @DisplayName("reportNonReceipt should require the completed student")
    void shouldRequireCompletedStudent() {
        Usuario student = createStudent("student-report-service-access@example.com");
        Material material = createMaterial(StatusMaterial.DOADO);

        when(usuarioRepository.findByEmailIgnoreCase(student.getEmail())).thenReturn(Optional.of(student));
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(solicitacaoRepository.findFirstByMaterialIdAndEstudanteIdAndStatusOrderByCriadoEmDesc(
                material.getId(),
                student.getId(),
                StatusSolicitacao.CONCLUIDA
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.reportNonReceipt(
                student.getEmail(),
                material.getId().toString(),
                new CreateNonReceiptReportRequestDTO("Nao chegou")
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Apenas o estudante com solicitacao concluida pode reportar nao recebimento");
    }

    private Usuario createStudent(String email) {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("hash")
                .nome("Estudante")
                .whatsapp("+5511991234567")
                .cidade("FLORIANOPOLIS")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .consentimentoIa(true)
                .role(Role.USER)
                .build();
    }

    private Material createMaterial(StatusMaterial status) {
        Usuario donor = Usuario.builder()
                .id(UUID.randomUUID())
                .email("donor@example.com")
                .passwordHash("hash")
                .nome("Doador")
                .whatsapp("+5511997654321")
                .cidade("FLORIANOPOLIS")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .role(Role.USER)
                .build();

        return Material.builder()
                .id(UUID.randomUUID())
                .doador(donor)
                .titulo("Colecao de teste")
                .descricao("Descricao de teste suficiente")
                .disciplina(Disciplina.MATEMATICA)
                .nivelEnsino(NivelEnsino.FUNDAMENTAL)
                .ano(7)
                .sistemaEnsino(SistemaEnsino.ANGLO)
                .estadoConservacao(EstadoConservacao.BOM)
                .status(status)
                .cidade("FLORIANOPOLIS")
                .bairro("CENTRO")
                .dataPublicacao(2024)
                .doadoEm(status == StatusMaterial.DOADO ? LocalDateTime.now().minusDays(1) : null)
                .build();
    }

    private Solicitacao createCompletedRequest(Material material, Usuario student) {
        return Solicitacao.builder()
                .id(UUID.randomUUID())
                .material(material)
                .estudante(student)
                .status(StatusSolicitacao.CONCLUIDA)
                .aprovadoEm(LocalDateTime.now().minusDays(2))
                .concluidoEm(LocalDateTime.now().minusDays(1))
                .build();
    }
}
