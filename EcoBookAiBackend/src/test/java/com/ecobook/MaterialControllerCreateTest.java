package com.ecobook;

import com.ecobook.model.TemporaryUpload;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Role;
import com.ecobook.model.enums.UploadProcessingStatus;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.TemporaryUploadRepository;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.security.JwtTokenProvider;
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
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaterialControllerCreateTest extends BaseIntegrationTest {

    private static final String SEEDED_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHiPpclJ3DgA6hfa/vw/jemjL5V6cO9e";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TemporaryUploadRepository temporaryUploadRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/materiais should create a material from a preview upload_id")
    void shouldCreateMaterialFromPreviewUpload() throws Exception {
        Usuario usuario = createCompleteUser("create-material@example.com", true);
        String uploadId = previewUploadIdFor(usuario);

        mockMvc.perform(post("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(usuario))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "upload_id": "%s",
                                  "titulo": "Colecao Anglo Matematica 7",
                                  "descricao": "Livro em bom estado, usado em 2024 e pronto para nova doacao.",
                                  "disciplina": "MATEMATICA",
                                  "nivel_ensino": "FUNDAMENTAL",
                                  "ano": 7,
                                  "sistema_ensino": "ANGLO",
                                  "estado_conservacao": "BOM",
                                  "data_publicacao": 2022
                                }
                                """.formatted(uploadId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Colecao Anglo Matematica 7"))
                .andExpect(jsonPath("$.descricao").value("Livro em bom estado, usado em 2024 e pronto para nova doacao."))
                .andExpect(jsonPath("$.status").value("DISPONIVEL"))
                .andExpect(jsonPath("$.upload_id").value(uploadId))
                .andExpect(jsonPath("$.imagem_url").value(org.hamcrest.Matchers.containsString("/api/uploads/")))
                .andExpect(jsonPath("$.doador.nome").value("Create User"))
                .andExpect(jsonPath("$.cidade").value("FLORIANOPOLIS"))
                .andExpect(jsonPath("$.bairro").value("CENTRO"));

        assertThat(materialRepository.findAll()).hasSize(1);
        assertThat(temporaryUploadRepository.findByUploadId(uploadId))
                .hasValueSatisfying(upload -> {
                    assertThat(upload.getStatus()).isEqualTo(UploadProcessingStatus.MATERIAL_CREATED);
                    assertThat(upload.getMaterial()).isNotNull();
                    assertThat(upload.getExpiresAt()).isNull();
                });
    }

    @Test
    @DisplayName("POST /api/v1/materiais should reject reused upload ids")
    void shouldRejectReusedUploadId() throws Exception {
        Usuario usuario = createCompleteUser("reused-upload@example.com", true);
        String uploadId = previewUploadIdFor(usuario);

        String payload = """
                {
                  "upload_id": "%s",
                  "titulo": "Livro de Historia",
                  "descricao": "Livro em bom estado, pronto para entrega e revisado manualmente.",
                  "disciplina": "HISTORIA",
                  "nivel_ensino": "MEDIO",
                  "ano": 2,
                  "sistema_ensino": "OUTRO",
                  "estado_conservacao": "USADO"
                }
                """.formatted(uploadId);

        mockMvc.perform(post("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(usuario))
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(usuario))
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Este upload_id ja foi utilizado"));
    }

    @Test
    @DisplayName("POST /api/v1/materiais should reject expired upload ids")
    void shouldRejectExpiredUploadId() throws Exception {
        Usuario usuario = createCompleteUser("expired-upload@example.com", true);
        String uploadId = previewUploadIdFor(usuario);

        TemporaryUpload upload = temporaryUploadRepository.findByUploadId(uploadId).orElseThrow();
        upload.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        temporaryUploadRepository.saveAndFlush(upload);

        mockMvc.perform(post("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(usuario))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "upload_id": "%s",
                                  "titulo": "Livro expirado",
                                  "descricao": "Descricao valida para validar expiracao do upload temporario.",
                                  "disciplina": "PORTUGUES",
                                  "nivel_ensino": "FUNDAMENTAL",
                                  "ano": 6,
                                  "sistema_ensino": "OUTRO",
                                  "estado_conservacao": "BOM"
                                }
                                """.formatted(uploadId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Temporary upload not found or expired"));
    }

    private Usuario createCompleteUser(String email, boolean consentimentoIa) {
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .email(email)
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Create User")
                .whatsapp("+5511991234567")
                .cidade("FLORIANOPOLIS")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .consentimentoIa(consentimentoIa)
                .role(Role.USER)
                .build());
    }

    private String previewUploadIdFor(Usuario usuario) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "colecao-anglo.png",
                IMAGE_PNG_VALUE,
                validPng()
        );

        String body = mockMvc.perform(multipart("/v1/materiais/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenFor(usuario)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        return json.path("upload_id").asText();
    }

    private String tokenFor(Usuario usuario) {
        return jwtTokenProvider.generateToken(
                usuario.getEmail(),
                usuario.getRole().name(),
                usuario.isPerfilCompleto(),
                usuario.getId().toString()
        );
    }

    private byte[] validPng() throws Exception {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setRGB(x, y, 0x0097A7);
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }
}
