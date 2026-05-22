package com.ecobook.controller;

import com.ecobook.model.Material;
import com.ecobook.model.MaterialNonReceiptReport;
import com.ecobook.model.Solicitacao;
import com.ecobook.model.TemporaryUpload;
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
import com.ecobook.repository.TemporaryUploadRepository;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.repository.projection.AdminUserMetricsProjection;
import com.ecobook.service.AdminPlatformService;
import com.ecobook.service.AdminReportService;
import com.ecobook.service.ImageStorageService;
import com.ecobook.service.MaterialMapper;
import com.ecobook.service.NotificationPayloadFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.ecobook.security.JwtAuthenticationFilter.class)
)
@Import({
        AdminControllerWebMvcTest.TestSecurityConfig.class,
        AdminReportService.class,
        AdminPlatformService.class,
        MaterialMapper.class,
        NotificationPayloadFactory.class,
        ImageStorageService.class
})
class AdminControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaterialNonReceiptReportRepository materialNonReceiptReportRepository;

    @MockBean
    private MaterialRepository materialRepository;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private SolicitacaoRepository solicitacaoRepository;

    @MockBean
    private TemporaryUploadRepository temporaryUploadRepository;

    @Test
    @DisplayName("GET /v1/admin/reports should return reports for admin users")
    @WithMockUser(roles = "ADMIN")
    void shouldListReportsForAdmins() throws Exception {
        MaterialNonReceiptReport report = sampleReport(StatusMaterial.DOADO, NonReceiptReportStatus.OPEN);
        PageRequest pageRequest = PageRequest.of(0, 20);

        when(materialNonReceiptReportRepository.findByStatusOrderByCreatedAtDesc(NonReceiptReportStatus.OPEN, pageRequest))
                .thenReturn(new PageImpl<>(List.of(report), pageRequest, 1));

        mockMvc.perform(get("/v1/admin/reports").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.results[0].id").value(report.getId().toString()))
                .andExpect(jsonPath("$.data.results[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("GET /v1/admin/reports should reject non-admin users")
    @WithMockUser(roles = "USER")
    void shouldRejectNonAdmins() throws Exception {
        mockMvc.perform(get("/v1/admin/reports"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(
                materialNonReceiptReportRepository,
                materialRepository,
                usuarioRepository,
                solicitacaoRepository,
                temporaryUploadRepository
        );
    }

    @Test
    @DisplayName("GET /v1/admin/reports should validate status filters")
    @WithMockUser(roles = "ADMIN")
    void shouldValidateStatusFilter() throws Exception {
        mockMvc.perform(get("/v1/admin/reports").param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field_errors.status").value("Use um dos valores: OPEN ou RESOLVED"));

        verifyNoInteractions(
                materialNonReceiptReportRepository,
                materialRepository,
                usuarioRepository,
                solicitacaoRepository,
                temporaryUploadRepository
        );
    }

    @Test
    @DisplayName("PATCH /v1/admin/reports/{id}/resolve should resolve reports for admin users")
    @WithMockUser(roles = "ADMIN")
    void shouldResolveReportsForAdmins() throws Exception {
        MaterialNonReceiptReport report = sampleReport(StatusMaterial.DOADO, NonReceiptReportStatus.OPEN);

        when(materialNonReceiptReportRepository.findDetailedById(report.getId())).thenReturn(Optional.of(report));
        when(materialNonReceiptReportRepository.save(any(MaterialNonReceiptReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(patch("/v1/admin/reports/{id}/resolve", report.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resolutionNotes": "Contato validado"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolution_notes").value("Contato validado"));
    }

    @Test
    @DisplayName("GET /v1/admin/materials should return paged materials for admin users")
    @WithMockUser(roles = "ADMIN")
    void shouldListMaterialsForAdmins() throws Exception {
        Material material = sampleMaterial(StatusMaterial.CANCELADO);
        PageRequest pageRequest = PageRequest.of(0, 20);

        when(materialRepository.findByStatusOrderByCriadoEmDesc(StatusMaterial.CANCELADO, pageRequest))
                .thenReturn(new PageImpl<>(List.of(material), pageRequest, 1));

        mockMvc.perform(get("/v1/admin/materials").param("status", "CANCELADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].status").value("CANCELADO"))
                .andExpect(jsonPath("$.data.results[0].doador.nome").value("Doador Admin"));
    }

    @Test
    @DisplayName("DELETE /v1/admin/materials/{id} should delete materials for admin users")
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteMaterialsForAdmins() throws Exception {
        Material material = sampleMaterial(StatusMaterial.DISPONIVEL);
        Usuario student = sampleStudent("student-delete@example.com");
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

        mockMvc.perform(delete("/v1/admin/materials/{id}", material.getId()))
                .andExpect(status().isNoContent());

        verify(materialRepository).delete(material);
    }

    @Test
    @DisplayName("GET /v1/admin/users should return paged users with metrics for admin users")
    @WithMockUser(roles = "ADMIN")
    void shouldListUsersForAdmins() throws Exception {
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email("student-admin@example.com")
                .passwordHash("hash")
                .nome("Estudante Admin")
                .whatsapp("+5511991234567")
                .cidade("FLORIANOPOLIS")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .consentimentoIa(true)
                .role(Role.USER)
                .necessidadesAcademicas(Set.of())
                .criadoEm(LocalDateTime.now().minusDays(2))
                .atualizadoEm(LocalDateTime.now().minusDays(1))
                .build();
        PageRequest pageRequest = PageRequest.of(0, 20);

        when(usuarioRepository.findAllByOrderByCriadoEmDesc(pageRequest))
                .thenReturn(new PageImpl<>(List.of(usuario), pageRequest, 1));
        when(usuarioRepository.findAdminMetricsByIds(
                List.of(usuario.getId()),
                StatusMaterial.DOADO,
                StatusSolicitacao.CONCLUIDA,
                NonReceiptReportStatus.OPEN
        )).thenReturn(List.of(adminMetricsProjection(usuario.getId(), 2L, 1L, 3L, 1L, 1L)));

        mockMvc.perform(get("/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].email").value("student-admin@example.com"))
                .andExpect(jsonPath("$.data.results[0].materials_count").value(2))
                .andExpect(jsonPath("$.data.results[0].open_reports_count").value(1));
    }

    private MaterialNonReceiptReport sampleReport(StatusMaterial materialStatus, NonReceiptReportStatus reportStatus) {
        Usuario donor = sampleDonor();
        Usuario student = sampleStudent("student-admin@example.com");
        Material material = sampleMaterial(materialStatus);
        material.setDoador(donor);

        Solicitacao solicitacao = Solicitacao.builder()
                .id(UUID.randomUUID())
                .material(material)
                .estudante(student)
                .status(StatusSolicitacao.CONCLUIDA)
                .build();

        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);

        return MaterialNonReceiptReport.builder()
                .id(UUID.randomUUID())
                .material(material)
                .solicitacao(solicitacao)
                .estudante(student)
                .reason("Nao chegou")
                .status(reportStatus)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    private Material sampleMaterial(StatusMaterial status) {
        return Material.builder()
                .id(UUID.randomUUID())
                .doador(sampleDonor())
                .titulo("Colecao reportada")
                .autor("Autor")
                .editora("Editora")
                .descricao("Descricao suficiente para o material administrativo")
                .disciplina(Disciplina.MATEMATICA)
                .nivelEnsino(NivelEnsino.FUNDAMENTAL)
                .ano(7)
                .sistemaEnsino(SistemaEnsino.ANGLO)
                .estadoConservacao(EstadoConservacao.BOM)
                .status(status)
                .cidade("FLORIANOPOLIS")
                .bairro("CENTRO")
                .dataPublicacao(2024)
                .criadoEm(LocalDateTime.now().minusDays(3))
                .atualizadoEm(LocalDateTime.now().minusDays(1))
                .build();
    }

    private Usuario sampleDonor() {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email("donor-admin@example.com")
                .passwordHash("hash")
                .nome("Doador Admin")
                .whatsapp("+5511997654321")
                .cidade("FLORIANOPOLIS")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .role(Role.USER)
                .build();
    }

    private Usuario sampleStudent(String email) {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("hash")
                .nome("Estudante Admin")
                .whatsapp("+5511991234567")
                .cidade("FLORIANOPOLIS")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .role(Role.USER)
                .build();
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

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }
    }
}
