package com.ecobook.service;

import com.ecobook.dto.AdminUserSummaryDTO;
import com.ecobook.dto.MaterialDTO;
import com.ecobook.dto.PagedResponseDTO;
import com.ecobook.event.NotificationRequestedEvent;
import com.ecobook.model.Material;
import com.ecobook.model.Solicitacao;
import com.ecobook.model.TemporaryUpload;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.NonReceiptReportStatus;
import com.ecobook.model.enums.Role;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.model.enums.StatusSolicitacao;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.SolicitacaoRepository;
import com.ecobook.repository.TemporaryUploadRepository;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.repository.projection.AdminUserMetricsProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminPlatformServiceTest {

    private final MaterialRepository materialRepository = mock(MaterialRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final SolicitacaoRepository solicitacaoRepository = mock(SolicitacaoRepository.class);
    private final TemporaryUploadRepository temporaryUploadRepository = mock(TemporaryUploadRepository.class);
    private final MaterialMapper materialMapper = new MaterialMapper();
    private final ImageStorageService imageStorageService = new ImageStorageService(temporaryUploadRepository);
    private final NotificationPayloadFactory notificationPayloadFactory = new NotificationPayloadFactory();
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private final AdminPlatformService adminPlatformService = new AdminPlatformService(
            materialRepository,
            usuarioRepository,
            solicitacaoRepository,
            temporaryUploadRepository,
            materialMapper,
            imageStorageService,
            notificationPayloadFactory,
            eventPublisher
    );

    @Test
    @DisplayName("listMaterials should return paged admin materials including canceled entries")
    void shouldListMaterials() {
        Material material = sampleMaterial(StatusMaterial.CANCELADO);
        PageRequest pageRequest = PageRequest.of(0, 20);

        when(materialRepository.findByStatusOrderByCriadoEmDesc(StatusMaterial.CANCELADO, pageRequest))
                .thenReturn(new PageImpl<>(List.of(material), pageRequest, 1));

        PagedResponseDTO<MaterialDTO> response = adminPlatformService.listMaterials(StatusMaterial.CANCELADO, pageRequest);

        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().getFirst().getStatus()).isEqualTo("CANCELADO");
    }

    @Test
    @DisplayName("deleteMaterial should cleanup tracked files and notify related users")
    void shouldDeleteMaterial() {
        Material material = sampleMaterial(StatusMaterial.DOADO);
        Usuario student = sampleStudent();
        Solicitacao request = Solicitacao.builder()
                .id(UUID.randomUUID())
                .material(material)
                .estudante(student)
                .status(StatusSolicitacao.CONCLUIDA)
                .build();
        TemporaryUpload upload = TemporaryUpload.builder()
                .filePath("C:/tmp/front.jpg")
                .secondaryFilePath("C:/tmp/back.jpg")
                .build();

        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(solicitacaoRepository.findByMaterialId(material.getId())).thenReturn(List.of(request));
        when(temporaryUploadRepository.findByMaterialId(material.getId())).thenReturn(Optional.of(upload));

        adminPlatformService.deleteMaterial(material.getId().toString());

        verify(temporaryUploadRepository).delete(upload);
        verify(materialRepository).delete(material);

        ArgumentCaptor<NotificationRequestedEvent> captor = ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NotificationRequestedEvent::recipientUserId)
                .containsExactlyInAnyOrder(material.getDoador().getId(), student.getId());
    }

    @Test
    @DisplayName("listUsers should return activity metrics merged with user data")
    void shouldListUsersWithMetrics() {
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email("admin-list-user@example.com")
                .passwordHash("hash")
                .nome("Usuario Ativo")
                .whatsapp("+5511991234567")
                .cpf("52998224725")
                .cidade("FLORIANOPOLIS")
                .bairro("CENTRO")
                .instituicao("UFSC")
                .perfilCompleto(true)
                .consentimentoIa(true)
                .role(Role.USER)
                .necessidadesAcademicas(Set.of())
                .criadoEm(LocalDateTime.now().minusDays(2))
                .atualizadoEm(LocalDateTime.now().minusDays(1))
                .build();
        PageRequest pageRequest = PageRequest.of(0, 20);
        AdminUserMetricsProjection projection = adminMetricsProjection(usuario.getId(), 3L, 1L, 4L, 2L, 1L);

        when(usuarioRepository.findAllByOrderByCriadoEmDesc(pageRequest))
                .thenReturn(new PageImpl<>(List.of(usuario), pageRequest, 1));
        when(usuarioRepository.findAdminMetricsByIds(
                List.of(usuario.getId()),
                StatusMaterial.DOADO,
                StatusSolicitacao.CONCLUIDA,
                NonReceiptReportStatus.OPEN
        )).thenReturn(List.of(projection));

        PagedResponseDTO<AdminUserSummaryDTO> response = adminPlatformService.listUsers(pageRequest);

        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getResults()).hasSize(1);
        AdminUserSummaryDTO result = response.getResults().getFirst();
        assertThat(result.getEmail()).isEqualTo("admin-list-user@example.com");
        assertThat(result.getMaterialsCount()).isEqualTo(3);
        assertThat(result.getDonatedMaterialsCount()).isEqualTo(1);
        assertThat(result.getRequestsCount()).isEqualTo(4);
        assertThat(result.getCompletedRequestsCount()).isEqualTo(2);
        assertThat(result.getOpenReportsCount()).isEqualTo(1);
    }

    private AdminUserMetricsProjection adminMetricsProjection(UUID userId,
                                                              Long materialsCount,
                                                              Long donatedMaterialsCount,
                                                              Long requestsCount,
                                                              Long completedRequestsCount,
                                                              Long openReportsCount) {
        return new AdminUserMetricsProjection() {
            @Override
            public UUID getUserId() {
                return userId;
            }

            @Override
            public Long getMaterialsCount() {
                return materialsCount;
            }

            @Override
            public Long getDonatedMaterialsCount() {
                return donatedMaterialsCount;
            }

            @Override
            public Long getRequestsCount() {
                return requestsCount;
            }

            @Override
            public Long getCompletedRequestsCount() {
                return completedRequestsCount;
            }

            @Override
            public Long getOpenReportsCount() {
                return openReportsCount;
            }
        };
    }

    private Material sampleMaterial(StatusMaterial status) {
        Usuario donor = Usuario.builder()
                .id(UUID.randomUUID())
                .email("donor-admin-platform@example.com")
                .passwordHash("hash")
                .nome("Doador Plataforma")
                .whatsapp("+5511997654321")
                .cpf("52998224725")
                .cidade("FLORIANOPOLIS")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .role(Role.USER)
                .build();

        return Material.builder()
                .id(UUID.randomUUID())
                .doador(donor)
                .titulo("Colecao administrativa")
                .autor("Autor")
                .editora("Editora")
                .descricao("Descricao administrativa suficiente")
                .disciplina(com.ecobook.model.enums.Disciplina.MATEMATICA)
                .nivelEnsino(com.ecobook.model.enums.NivelEnsino.FUNDAMENTAL)
                .ano(7)
                .sistemaEnsino(com.ecobook.model.enums.SistemaEnsino.ANGLO)
                .estadoConservacao(com.ecobook.model.enums.EstadoConservacao.BOM)
                .status(status)
                .cidade("FLORIANOPOLIS")
                .bairro("CENTRO")
                .dataPublicacao(2024)
                .criadoEm(LocalDateTime.now().minusDays(1))
                .atualizadoEm(LocalDateTime.now())
                .build();
    }

    private Usuario sampleStudent() {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email("student-admin-platform@example.com")
                .passwordHash("hash")
                .nome("Estudante Plataforma")
                .whatsapp("+5511988887777")
                .cpf("52998224725")
                .cidade("FLORIANOPOLIS")
                .bairro("TRINDADE")
                .perfilCompleto(true)
                .role(Role.USER)
                .build();
    }
}
