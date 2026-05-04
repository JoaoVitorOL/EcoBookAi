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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioServiceTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("PUT /api/v1/usuarios/me should complete the user profile and normalize geo fields")
    void shouldUpdateProfile() throws Exception {
        String token = tokenFor(usuarioRepository.saveAndFlush(Usuario.builder()
                .email("profile@example.com")
                .nome("Profile User")
                .googleId("google-profile-123")
                .perfilCompleto(false)
                .role(Role.USER)
                .build()));

        mockMvc.perform(put("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Profile User",
                                  "whatsapp": "+5511991234567",
                                  "cidade": "São José dos Campos",
                                  "bairro": " centro ",
                                  "consentimento_ia": true,
                                  "necessidades_academicas": ["TEXTBOOKS", "TEST_PREP"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil_completo").value(true))
                .andExpect(jsonPath("$.cidade").value("SAO JOSE DOS CAMPOS"))
                .andExpect(jsonPath("$.bairro").value("CENTRO"))
                .andExpect(jsonPath("$.consentimento_ia").value(true));
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios/me should reject invalid WhatsApp format")
    void shouldRejectInvalidWhatsApp() throws Exception {
        String token = tokenFor(usuarioRepository.saveAndFlush(Usuario.builder()
                .email("invalid-whatsapp@example.com")
                .nome("Invalid WhatsApp")
                .googleId("google-invalid-whatsapp")
                .perfilCompleto(false)
                .role(Role.USER)
                .build()));

        mockMvc.perform(put("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Invalid WhatsApp",
                                  "whatsapp": "11991234567",
                                  "cidade": "Ribeirão Preto",
                                  "bairro": "Centro"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_FORMAT"));
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios/me should reject incomplete profile payloads")
    void shouldRejectIncompletePayload() throws Exception {
        String token = tokenFor(usuarioRepository.saveAndFlush(Usuario.builder()
                .email("incomplete@example.com")
                .nome("Incomplete User")
                .googleId("google-incomplete")
                .perfilCompleto(false)
                .role(Role.USER)
                .build()));

        mockMvc.perform(put("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Incomplete User",
                                  "whatsapp": "+5511991234567",
                                  "bairro": "Centro"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.field_errors.cidade").value("Cidade is required"));
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
