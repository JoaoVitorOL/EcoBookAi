package com.ecobook;

import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Role;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioControllerTest extends BaseIntegrationTest {

    private static final String SEEDED_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHiPpclJ3DgA6hfa/vw/jemjL5V6cO9e";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("GET /api/v1/usuarios/me should return the authenticated user")
    void shouldReturnCurrentUser() throws Exception {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("reader@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Reader User")
                .whatsapp("+5511991234567")
                .cpf("52998224725")
                .cidade("SAO PAULO")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .role(Role.USER)
                .build());

        String token = jwtTokenProvider.generateToken(
                usuario.getEmail(),
                usuario.getRole().name(),
                usuario.isPerfilCompleto(),
                usuario.getId().toString()
        );

        mockMvc.perform(get("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("reader@example.com"))
                .andExpect(jsonPath("$.data.perfil_completo").value(true))
                .andExpect(jsonPath("$.data.cidade").value("SAO PAULO"));
    }

    @Test
    @DisplayName("GET /api/v1/usuarios/me should require a JWT")
    void shouldRequireJwtForGetMe() throws Exception {
        mockMvc.perform(get("/v1/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GET /api/v1/usuarios/me should return 401 when the token email no longer exists")
    void shouldReturnNotFoundWhenTokenEmailDoesNotExist() throws Exception {
        String token = jwtTokenProvider.generateToken(
                "missing@example.com",
                Role.USER.name(),
                true,
                "missing-user"
        );

        mockMvc.perform(get("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Um token JWT valido e obrigatorio"));
    }

    @Test
    @DisplayName("PATCH /api/v1/usuarios/me/consentimento-ia should update AI consent without rewriting the rest of the profile")
    void shouldUpdateAiConsent() throws Exception {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("consent@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Consent User")
                .whatsapp("+5511991234567")
                .cpf("52998224725")
                .cidade("SAO PAULO")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .consentimentoIa(false)
                .role(Role.USER)
                .build());

        String token = jwtTokenProvider.generateToken(
                usuario.getEmail(),
                usuario.getRole().name(),
                usuario.isPerfilCompleto(),
                usuario.getId().toString()
        );

        mockMvc.perform(patch("/v1/usuarios/me/consentimento-ia")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "consentimento_ia": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("consent@example.com"))
                .andExpect(jsonPath("$.data.consentimento_ia").value(true))
                .andExpect(jsonPath("$.data.cidade").value("SAO PAULO"));
    }

    @Test
    @DisplayName("DELETE /api/v1/usuarios/me/consent/ai-classification should revoke AI consent")
    void shouldRevokeAiConsent() throws Exception {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("consent-revoke@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Consent Revoke User")
                .whatsapp("+5511991234567")
                .cpf("52998224725")
                .cidade("SAO PAULO")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .consentimentoIa(true)
                .role(Role.USER)
                .build());

        String token = jwtTokenProvider.generateToken(
                usuario.getEmail(),
                usuario.getRole().name(),
                usuario.isPerfilCompleto(),
                usuario.getId().toString()
        );

        mockMvc.perform(delete("/v1/usuarios/me/consent/ai-classification")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("consent-revoke@example.com"))
                .andExpect(jsonPath("$.data.consentimento_ia").value(false));
    }
}
