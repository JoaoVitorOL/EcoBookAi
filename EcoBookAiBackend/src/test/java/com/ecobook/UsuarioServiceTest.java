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

    private static final String SEEDED_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHiPpclJ3DgA6hfa/vw/jemjL5V6cO9e";

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
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Profile User")
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
                                  "cidade": "Sao Jose",
                                  "bairro": " centro ",
                                  "consentimento_ia": true,
                                  "necessidades_academicas": ["TEXTBOOKS", "TEST_PREP"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.perfil_completo").value(true))
                .andExpect(jsonPath("$.data.cidade").value("SAO JOSE"))
                .andExpect(jsonPath("$.data.bairro").value("CENTRO"))
                .andExpect(jsonPath("$.data.consentimento_ia").value(true));
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios/me should reject invalid WhatsApp format")
    void shouldRejectInvalidWhatsApp() throws Exception {
        String token = tokenFor(usuarioRepository.saveAndFlush(Usuario.builder()
                .email("invalid-whatsapp@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Invalid WhatsApp")
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
                                  "cidade": "Criciuma",
                                  "bairro": "Centro"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_FORMAT"))
                .andExpect(jsonPath("$.field_errors.whatsapp").exists());
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios/me should reject incomplete profile payloads")
    void shouldRejectIncompletePayload() throws Exception {
        String token = tokenFor(usuarioRepository.saveAndFlush(Usuario.builder()
                .email("incomplete@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Incomplete User")
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
                .andExpect(jsonPath("$.field_errors.cidade").value("Informe sua cidade"));
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios/me should trim optional institution and default optional profile flags")
    void shouldTrimInstitutionAndDefaultOptionalProfileFlags() throws Exception {
        String token = tokenFor(usuarioRepository.saveAndFlush(Usuario.builder()
                .email("institution@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Institution User")
                .perfilCompleto(false)
                .role(Role.USER)
                .build()));

        mockMvc.perform(put("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Institution User",
                                  "whatsapp": "+5511991234567",
                                  "cidade": "Curitiba",
                                  "bairro": "Centro",
                                  "instituicao": "  IFPR Curitiba  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instituicao").value("IFPR Curitiba"))
                .andExpect(jsonPath("$.data.consentimento_ia").value(false))
                .andExpect(jsonPath("$.data.necessidades_academicas").isArray())
                .andExpect(jsonPath("$.data.necessidades_academicas").isEmpty());

        usuarioRepository.findByEmailIgnoreCase("institution@example.com")
                .ifPresent(usuario -> {
                    org.assertj.core.api.Assertions.assertThat(usuario.getInstituicao()).isEqualTo("IFPR Curitiba");
                    org.assertj.core.api.Assertions.assertThat(usuario.getConsentimentoIa()).isFalse();
                    org.assertj.core.api.Assertions.assertThat(usuario.getNecessidadesAcademicas()).isEmpty();
                });
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios/me should reject blank required fields after trimming")
    void shouldRejectBlankRequiredFields() throws Exception {
        String token = tokenFor(usuarioRepository.saveAndFlush(Usuario.builder()
                .email("blank-fields@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Blank Fields User")
                .perfilCompleto(false)
                .role(Role.USER)
                .build()));

        mockMvc.perform(put("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "   ",
                                  "whatsapp": "+5511991234567",
                                  "cidade": "Curitiba",
                                  "bairro": "Centro"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("UNPROCESSABLE_ENTITY"))
                .andExpect(jsonPath("$.field_errors.nome").value("Informe seu nome"));
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios/me should accept free-text cities and normalize them before saving")
    void shouldAcceptFreeTextCityAndNormalizeIt() throws Exception {
        String token = tokenFor(usuarioRepository.saveAndFlush(Usuario.builder()
                .email("free-text-city@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Free Text City User")
                .perfilCompleto(false)
                .role(Role.USER)
                .build()));

        mockMvc.perform(put("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Free Text City User",
                                  "whatsapp": "+5511991234567",
                                  "cidade": " Ribeirão Preto ",
                                  "bairro": " Jardim Botanico "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cidade").value("RIBEIRAO PRETO"))
                .andExpect(jsonPath("$.data.bairro").value("JARDIM BOTANICO"));
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
