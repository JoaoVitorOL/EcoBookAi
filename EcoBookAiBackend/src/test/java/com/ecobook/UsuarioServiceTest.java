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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @DisplayName("PUT /api/v1/usuarios/me should complete the user profile and normalize only the city field")
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
                                  "cpf": "52998224725",
                                  "cidade": "Sao Jose",
                                  "bairro": " centro ",
                                  "consentimento_ia": true,
                                  "necessidades_academicas": ["TEXTBOOKS", "TEST_PREP"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.perfil_completo").value(true))
                .andExpect(jsonPath("$.data.cidade").value("SAO JOSE"))
                .andExpect(jsonPath("$.data.bairro").value("centro"))
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
                                  "cpf": "52998224725",
                                  "cidade": "Criciuma",
                                  "bairro": "Centro"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_FORMAT"))
                .andExpect(jsonPath("$.field_errors.whatsapp").exists());
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios/me should reject WhatsApp values with formatting characters")
    void shouldRejectWhatsAppWithFormattingCharacters() throws Exception {
        String token = tokenFor(usuarioRepository.saveAndFlush(Usuario.builder()
                .email("formatted-whatsapp@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Formatted WhatsApp")
                .perfilCompleto(false)
                .role(Role.USER)
                .build()));

        mockMvc.perform(put("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Formatted WhatsApp",
                                  "whatsapp": "+55 (11) 99123-4567",
                                  "cpf": "52998224725",
                                  "cidade": "Criciuma",
                                  "bairro": "Centro"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_FORMAT"))
                .andExpect(jsonPath("$.field_errors.whatsapp").value("Use o formato E.164, por exemplo +5511999999999"));
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
                                  "cpf": "52998224725",
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
                                  "cpf": "52998224725",
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
                                  "cpf": "52998224725",
                                  "cidade": "Curitiba",
                                  "bairro": "Centro"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("UNPROCESSABLE_ENTITY"))
                .andExpect(jsonPath("$.field_errors.nome").value("Informe seu nome"));
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios/me should accept free-text cities and keep neighborhood accentuation")
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
                                  "cpf": "52998224725",
                                  "cidade": " Ribeirao Preto ",
                                  "bairro": " Jardim Botanico "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cidade").value("RIBEIRAO PRETO"))
                .andExpect(jsonPath("$.data.bairro").value("Jardim Botanico"));
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios/me should normalize Unicode city names without stripping neighborhood accents")
    void shouldNormalizeUnicodeCityNames() throws Exception {
        String token = tokenFor(usuarioRepository.saveAndFlush(Usuario.builder()
                .email("unicode-city@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Unicode City User")
                .perfilCompleto(false)
                .role(Role.USER)
                .build()));

        mockMvc.perform(put("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Unicode City User",
                                  "whatsapp": "+5511991234567",
                                  "cpf": "52998224725",
                                  "cidade": " São José ",
                                  "bairro": " Trindade açoriana "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cidade").value("SAO JOSE"))
                .andExpect(jsonPath("$.data.bairro").value("Trindade açoriana"));
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios/me should allow changing the email and require reauthentication afterwards")
    void shouldUpdateEmailAndInvalidateOldJwtIdentity() throws Exception {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("before-update@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Email Update User")
                .perfilCompleto(false)
                .role(Role.USER)
                .build());

        String token = tokenFor(usuario);

        mockMvc.perform(put("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "NEW-EMAIL@example.com",
                                  "nome": "Email Update User",
                                  "whatsapp": "+5511991234567",
                                  "cpf": "52998224725",
                                  "cidade": "Florianopolis",
                                  "bairro": "Centro"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("new-email@example.com"));

        mockMvc.perform(get("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios/me should reject duplicate emails")
    void shouldRejectDuplicateEmail() throws Exception {
        usuarioRepository.saveAndFlush(Usuario.builder()
                .email("existing@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Existing User")
                .whatsapp("+5511991234567")
                .cpf("52998224725")
                .cidade("CURITIBA")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .role(Role.USER)
                .build());

        String token = tokenFor(usuarioRepository.saveAndFlush(Usuario.builder()
                .email("editable@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Editable User")
                .perfilCompleto(false)
                .role(Role.USER)
                .build()));

        mockMvc.perform(put("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "existing@example.com",
                                  "nome": "Editable User",
                                  "whatsapp": "+5511991234567",
                                  "cpf": "52998224725",
                                  "cidade": "Curitiba",
                                  "bairro": "Centro"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_FORMAT"))
                .andExpect(jsonPath("$.field_errors.email").value("Este email já está cadastrado"));
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
