package com.ecobook.controller;

import com.ecobook.dto.UpdateAiConsentRequestDTO;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Role;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.service.GeoNormalizationService;
import com.ecobook.service.UsuarioService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UsuarioController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.ecobook.security.JwtAuthenticationFilter.class)
)
@Import({
        UsuarioControllerWebMvcTest.TestSecurityConfig.class,
        UsuarioService.class,
        GeoNormalizationService.class
})
class UsuarioControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private Validator validator;

    @MockBean
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("PATCH /v1/usuarios/me/consentimento-ia should update AI consent")
    @WithMockUser(username = "consent-webmvc@example.com", roles = "USER")
    void shouldUpdateAiConsent() throws Exception {
        when(usuarioRepository.findByEmailIgnoreCase("consent-webmvc@example.com"))
                .thenReturn(Optional.of(sampleUser("consent-webmvc@example.com", false)));
        when(usuarioRepository.save(org.mockito.ArgumentMatchers.any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(patch("/v1/usuarios/me/consentimento-ia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "consentimento_ia": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("consent-webmvc@example.com"))
                .andExpect(jsonPath("$.data.consentimento_ia").value(true));
    }

    @Test
    @DisplayName("DELETE /v1/usuarios/me/consent/ai-classification should revoke AI consent")
    @WithMockUser(username = "consent-revoke-webmvc@example.com", roles = "USER")
    void shouldRevokeAiConsent() throws Exception {
        when(usuarioRepository.findByEmailIgnoreCase("consent-revoke-webmvc@example.com"))
                .thenReturn(Optional.of(sampleUser("consent-revoke-webmvc@example.com", true)));
        when(usuarioRepository.save(org.mockito.ArgumentMatchers.any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(delete("/v1/usuarios/me/consent/ai-classification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("consent-revoke-webmvc@example.com"))
                .andExpect(jsonPath("$.data.consentimento_ia").value(false));
    }

    private Usuario sampleUser(String email, boolean consentimentoIa) {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("hash")
                .nome("Usuario Consentimento")
                .whatsapp("+5511991234567")
                .cidade("SAO PAULO")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .consentimentoIa(consentimentoIa)
                .role(Role.USER)
                .necessidadesAcademicas(Set.of())
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
