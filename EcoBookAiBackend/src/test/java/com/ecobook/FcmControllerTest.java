package com.ecobook;

import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Role;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FcmControllerTest extends BaseIntegrationTest {

    private static final String SEEDED_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHiPpclJ3DgA6hfa/vw/jemjL5V6cO9e";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("POST /api/v1/fcm/tokens should persist the authenticated device token")
    void shouldPersistAuthenticatedDeviceToken() throws Exception {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("fcm@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("FCM User")
                .whatsapp("+5511991234567")
                .cpf("52998224725")
                .cidade("SAO PAULO")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .role(Role.USER)
                .build());

                mockMvc.perform(post("/v1/fcm/tokens")
                        .header("Authorization", "Bearer " + tokenFor(usuario))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "   token-device-123   "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token FCM sincronizado com sucesso"));

        Usuario savedUser = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(savedUser.getFcmToken()).isEqualTo("token-device-123");
    }

    @Test
    @DisplayName("POST /api/v1/fcm/tokens should require JWT authentication")
    void shouldRequireJwtToPersistDeviceToken() throws Exception {
        mockMvc.perform(post("/v1/fcm/tokens")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "token-device-123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("POST /api/v1/fcm/tokens should reject blank tokens")
    void shouldRejectBlankTokens() throws Exception {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("blank-fcm@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Blank FCM User")
                .whatsapp("+5511991234567")
                .cpf("52998224725")
                .cidade("SAO PAULO")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .role(Role.USER)
                .build());

        mockMvc.perform(post("/v1/fcm/tokens")
                        .header("Authorization", "Bearer " + tokenFor(usuario))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.field_errors.token").exists());
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
