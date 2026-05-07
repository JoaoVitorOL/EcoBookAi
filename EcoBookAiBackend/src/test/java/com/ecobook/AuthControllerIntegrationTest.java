package com.ecobook;

import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Role;
import com.ecobook.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends BaseIntegrationTest {

    private static final String LEGACY_PLACEHOLDER_HASH =
            "$2a$10$zKy.OSOH8QwuPcygx3gSbeqZesX.A4MLu7YAMTLmNPfx139CWSFKW";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("POST /api/v1/auth/register should create a user and return the platform JWT")
    void shouldRegisterUserWithEmailAndPassword() throws Exception {
        String responseBody = mockMvc.perform(post("/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "newuser@example.com",
                                  "password": "SenhaSegura123",
                                  "nome": "New User"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.data.nome").value("New User"))
                .andExpect(jsonPath("$.data.perfil_completo").value(false))
                .andExpect(jsonPath("$.data.consentimento_ia").value(false))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(responseBody);
        assertThat(usuarioRepository.findByEmailIgnoreCase("newuser@example.com"))
                .hasValueSatisfying(usuario -> {
                    assertThat(usuario.getPasswordHash()).isNotBlank();
                    assertThat(usuario.getPasswordHash()).isNotEqualTo("SenhaSegura123");
                    assertThat(passwordEncoder.matches("SenhaSegura123", usuario.getPasswordHash())).isTrue();
                });
        assertThat(jsonNode.path("data").path("expires_in").asLong()).isPositive();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 409 when the email is already registered")
    void shouldRejectDuplicateEmailRegistration() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "duplicate@example.com",
                                  "password": "SenhaSegura123",
                                  "nome": "First User"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "duplicate@example.com",
                                  "password": "OutraSenha123",
                                  "nome": "Second User"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Este email ja esta cadastrado"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should reject auto-claiming a legacy placeholder account")
    void shouldRejectLegacyPlaceholderAccountClaim() throws Exception {
        usuarioRepository.save(Usuario.builder()
                .email("legacy@example.com")
                .passwordHash(LEGACY_PLACEHOLDER_HASH)
                .nome("Legacy User")
                .role(Role.USER)
                .perfilCompleto(false)
                .consentimentoIa(false)
                .build());

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "legacy@example.com",
                                  "password": "SenhaSegura123",
                                  "nome": "Claim Attempt"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Este email ja esta cadastrado"));

        assertThat(usuarioRepository.findByEmailIgnoreCase("legacy@example.com"))
                .hasValueSatisfying(usuario ->
                        assertThat(usuario.getPasswordHash()).isEqualTo(LEGACY_PLACEHOLDER_HASH));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should authenticate an existing user and return the platform JWT")
    void shouldLoginWithEmailAndPassword() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "existing@example.com",
                                  "password": "SenhaSegura123",
                                  "nome": "Existing User"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "existing@example.com",
                                  "password": "SenhaSegura123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("existing@example.com"))
                .andExpect(jsonPath("$.data.perfil_completo").value(false))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 401 when the password is invalid")
    void shouldRejectInvalidPassword() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@example.com",
                                  "password": "SenhaSegura123",
                                  "nome": "Login User"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@example.com",
                                  "password": "SenhaErrada123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Email ou senha invalidos"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should normalize email casing and trim the user name")
    void shouldNormalizeRegistrationFields() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "MixedCase@Example.COM",
                                  "password": "SenhaSegura123",
                                  "nome": "  Mixed User  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("mixedcase@example.com"))
                .andExpect(jsonPath("$.data.nome").value("Mixed User"));

        assertThat(usuarioRepository.findByEmailIgnoreCase("mixedcase@example.com"))
                .hasValueSatisfying(usuario -> assertThat(usuario.getNome()).isEqualTo("Mixed User"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should accept the same email with different casing")
    void shouldLoginWithCaseInsensitiveEmail() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "caseuser@example.com",
                                  "password": "SenhaSegura123",
                                  "nome": "Case User"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "CASEUSER@EXAMPLE.COM",
                                  "password": "SenhaSegura123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("caseuser@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should reject invalid payloads before reaching the service layer")
    void shouldRejectInvalidRegistrationPayload() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid-email",
                                  "password": "123",
                                  "nome": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.field_errors.email").value("Informe um email valido"))
                .andExpect(jsonPath("$.field_errors.password").value("A senha deve ter entre 8 e 72 caracteres"))
                .andExpect(jsonPath("$.field_errors.nome").value("Informe seu nome"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should reject malformed payloads with validation errors")
    void shouldRejectInvalidLoginPayload() throws Exception {
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "existing@example.com",
                                  "password": "123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.field_errors.password").value("A senha deve ter entre 8 e 72 caracteres"));
    }
}
