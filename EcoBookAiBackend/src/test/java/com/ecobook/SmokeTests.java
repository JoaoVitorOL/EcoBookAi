package com.ecobook;

import com.ecobook.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SmokeTests extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("smoke should keep public health endpoints online")
    void shouldKeepHealthEndpointsOnline() throws Exception {
        mockMvc.perform(get("/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("smoke should keep OpenAPI docs and Swagger UI publicly reachable")
    void shouldKeepOpenApiDocumentationAvailable() throws Exception {
        String apiDocsPayload = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(apiDocsPayload).contains("EcoBook AI Backend API");
        assertThat(apiDocsPayload).contains("bearer-jwt");
        assertThat(apiDocsPayload).contains("/v1/auth/login");

        String swaggerUiPayload = mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(swaggerUiPayload).contains("Swagger UI");
    }

    @Test
    @DisplayName("smoke should keep auth, profile and discovery endpoints functional")
    void shouldKeepAuthProfileAndDiscoveryFunctional() throws Exception {
        String token = registerCompleteUserAndExtractToken("smoke-flow@example.com", "Smoke Flow");

        mockMvc.perform(get("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("smoke-flow@example.com"));

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results").isArray());
    }

    @Test
    @DisplayName("smoke should keep preview and Prometheus metrics available")
    void shouldKeepPreviewAndPrometheusMetricsAvailable() throws Exception {
        String token = registerCompleteUserAndExtractToken("smoke-preview@example.com", "Smoke Preview");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "matematica-smoke.png",
                IMAGE_PNG_VALUE,
                validPng()
        );

        mockMvc.perform(multipart("/v1/materiais/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.upload_id").isNotEmpty());

        String metricsPayload = mockMvc.perform(get("/actuator/prometheus")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(metricsPayload).contains("http_server_requests_seconds_count");
        assertThat(metricsPayload).contains("hikaricp_connections");
        assertThat(metricsPayload).contains("jvm_threads_live_threads");
    }

    private String registerAndExtractToken(String email, String nome) throws Exception {
        String registerResponse = mockMvc.perform(post("/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "SenhaSegura123",
                                  "nome": "%s"
                                }
                                """.formatted(email, nome)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerResponse);
        String token = registerJson.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private String registerCompleteUserAndExtractToken(String email, String nome) throws Exception {
        String token = registerAndExtractToken(email, nome);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "%s",
                                  "whatsapp": "+5511991234567",
                                  "cidade": "Florianopolis",
                                  "bairro": "Centro",
                                  "instituicao": "Escola Smoke",
                                  "consentimento_ia": true,
                                  "necessidades_academicas": ["TEXTBOOKS"]
                                }
                                """.formatted(nome)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.perfil_completo").value(true));

        assertThat(usuarioRepository.findByEmailIgnoreCase(email)).isPresent();
        return token;
    }

    private byte[] validPng() throws Exception {
        BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setRGB(x, y, 0x00A86B);
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }
}
