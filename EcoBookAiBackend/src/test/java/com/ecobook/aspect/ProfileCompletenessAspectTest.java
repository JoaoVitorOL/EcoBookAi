package com.ecobook.aspect;

import com.ecobook.BaseIntegrationTest;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Role;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileCompletenessAspectTest extends BaseIntegrationTest {

    private static final String SEEDED_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHiPpclJ3DgA6hfa/vw/jemjL5V6cO9e";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Protected material endpoints should reject users with incomplete profile")
    void shouldBlockIncompleteProfiles() throws Exception {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("blocked@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Blocked User")
                .perfilCompleto(false)
                .role(Role.USER)
                .build());

        mockMvc.perform(post("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(usuario)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("INCOMPLETE_PROFILE"));
    }

    @Test
    @DisplayName("Protected material endpoints should proceed when the profile is complete")
    void shouldAllowCompleteProfiles() throws Exception {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("allowed@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Allowed User")
                .whatsapp("+5511991234567")
                .cidade("SAO PAULO")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .role(Role.USER)
                .build());

        mockMvc.perform(post("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(usuario)))
                .andExpect(status().isNotImplemented());
    }

    @Test
    @DisplayName("Material preview skeleton should already be reachable for complete profiles")
    void shouldExposePreviewSkeletonForCompleteProfiles() throws Exception {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("preview@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Preview User")
                .whatsapp("+5511991234567")
                .cidade("SAO PAULO")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .role(Role.USER)
                .build());

        mockMvc.perform(post("/v1/materiais/preview")
                        .header("Authorization", "Bearer " + tokenFor(usuario)))
                .andExpect(status().isNotImplemented());
    }

    private String tokenFor(Usuario usuario) {
        return jwtTokenProvider.generateToken(
                usuario.getEmail(),
                usuario.getRole().name(),
                usuario.isPerfilCompleto(),
                usuario.getId().toString()
        );
    }
}
