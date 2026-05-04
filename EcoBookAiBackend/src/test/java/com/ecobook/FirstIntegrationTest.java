package com.ecobook;

import com.ecobook.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FirstIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockBean(name = "googleJwtDecoder")
    private JwtDecoder googleJwtDecoder;

    @Test
    @DisplayName("Register flow should issue a JWT, expose the user, and keep health/database checks passing")
    public void testApplicationStartup() throws Exception {
        when(googleJwtDecoder.decode("phase2-google-token")).thenReturn(
                Jwt.withTokenValue("phase2-google-token")
                        .header("alg", "RS256")
                        .subject("google-flow-123")
                        .issuer("https://accounts.google.com")
                        .claim("email", "flow@example.com")
                        .claim("name", "Flow User")
                        .claim("email_verified", true)
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300))
                        .build()
        );

        String registerResponse = mockMvc.perform(post("/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "google_token": "phase2-google-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil_completo").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerResponse);
        String appToken = registerJson.path("token").asText();

        mockMvc.perform(get("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + appToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("flow@example.com"))
                .andExpect(jsonPath("$.perfil_completo").value(false));

        mockMvc.perform(get("/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        Integer databaseProbe = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(databaseProbe).isEqualTo(1);
        assertThat(usuarioRepository.findByEmailIgnoreCase("flow@example.com")).isPresent();
    }
}
