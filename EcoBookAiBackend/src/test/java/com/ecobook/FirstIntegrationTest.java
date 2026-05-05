package com.ecobook;

import com.ecobook.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.http.MediaType.APPLICATION_JSON;
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

    @Test
    @DisplayName("Register flow should issue a JWT, expose the user, and keep health/database checks passing")
    public void testApplicationStartup() throws Exception {
        String registerResponse = mockMvc.perform(post("/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "flow@example.com",
                                  "password": "SenhaSegura123",
                                  "nome": "Flow User"
                                }
                                """))
                .andExpect(status().isCreated())
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
