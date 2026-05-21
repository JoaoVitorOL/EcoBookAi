package com.ecobook.controller;

import com.ecobook.model.Material;
import com.ecobook.model.MaterialNonReceiptReport;
import com.ecobook.model.Solicitacao;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.NonReceiptReportStatus;
import com.ecobook.model.enums.Role;
import com.ecobook.repository.MaterialNonReceiptReportRepository;
import com.ecobook.service.AdminReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.FilterType;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.ecobook.security.JwtAuthenticationFilter.class)
)
@Import({AdminControllerWebMvcTest.TestSecurityConfig.class, AdminReportService.class})
class AdminControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaterialNonReceiptReportRepository materialNonReceiptReportRepository;

    @Test
    @DisplayName("GET /v1/admin/reports should return reports for admin users")
    @WithMockUser(roles = "ADMIN")
    void shouldListReportsForAdmins() throws Exception {
        MaterialNonReceiptReport report = sampleReport();
        when(materialNonReceiptReportRepository.findByStatusOrderByCreatedAtDesc(NonReceiptReportStatus.OPEN, org.springframework.data.domain.PageRequest.of(0, 20)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(report), org.springframework.data.domain.PageRequest.of(0, 20), 1));

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

        verifyNoInteractions(materialNonReceiptReportRepository);
    }

    @Test
    @DisplayName("GET /v1/admin/reports should validate status filters")
    @WithMockUser(roles = "ADMIN")
    void shouldValidateStatusFilter() throws Exception {
        mockMvc.perform(get("/v1/admin/reports").param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field_errors.status").value("Use um dos valores: OPEN ou RESOLVED"));

        verifyNoInteractions(materialNonReceiptReportRepository);
    }

    @Test
    @DisplayName("PATCH /v1/admin/reports/{id}/resolve should resolve reports for admin users")
    @WithMockUser(roles = "ADMIN")
    void shouldResolveReportsForAdmins() throws Exception {
        MaterialNonReceiptReport report = sampleReport();
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

    private MaterialNonReceiptReport sampleReport() {
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
                .status(NonReceiptReportStatus.OPEN)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
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
