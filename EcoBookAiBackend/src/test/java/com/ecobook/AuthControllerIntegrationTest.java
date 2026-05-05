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
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.nome").value("New User"))
                .andExpect(jsonPath("$.perfil_completo").value(false))
                .andExpect(jsonPath("$.consentimento_ia").value(false))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.token").isNotEmpty())
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
        assertThat(jsonNode.path("expires_in").asLong()).isPositive();
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
                .andExpect(jsonPath("$.email").value("existing@example.com"))
                .andExpect(jsonPath("$.perfil_completo").value(false))
                .andExpect(jsonPath("$.token").isNotEmpty());
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
}
