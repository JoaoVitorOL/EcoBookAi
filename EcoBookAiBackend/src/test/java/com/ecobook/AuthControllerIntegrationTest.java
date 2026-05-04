package com.ecobook;

import com.ecobook.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockBean(name = "googleJwtDecoder")
    private JwtDecoder googleJwtDecoder;

    @Test
    @DisplayName("POST /api/v1/auth/register should create a user and return the platform JWT")
    void shouldRegisterUserWithValidGoogleToken() throws Exception {
        when(googleJwtDecoder.decode("valid-google-token")).thenReturn(validGoogleJwt());

        String responseBody = mockMvc.perform(post("/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "google_token": "valid-google-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.nome").value("New User"))
                .andExpect(jsonPath("$.perfil_completo").value(false))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(responseBody);
        assertThat(usuarioRepository.findByEmailIgnoreCase("newuser@example.com")).isPresent();
        assertThat(jsonNode.path("expires_in").asLong()).isPositive();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 401 when the Google token is invalid")
    void shouldRejectInvalidGoogleToken() throws Exception {
        when(googleJwtDecoder.decode("invalid-google-token")).thenThrow(new JwtException("bad token"));

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "google_token": "invalid-google-token"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    private Jwt validGoogleJwt() {
        return Jwt.withTokenValue("valid-google-token")
                .header("alg", "RS256")
                .subject("google-user-123")
                .issuer("https://accounts.google.com")
                .claim("aud", List.of())
                .claim("email", "newuser@example.com")
                .claim("name", "New User")
                .claim("email_verified", true)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }
}
