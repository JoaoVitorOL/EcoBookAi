package com.ecobook;

import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Role;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaterialControllerPreviewTest extends BaseIntegrationTest {

    private static final String SEEDED_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHiPpclJ3DgA6hfa/vw/jemjL5V6cO9e";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("POST /api/v1/materiais/preview should classify a valid image and return an upload id")
    void shouldPreviewValidImage() throws Exception {
        Usuario usuario = createCompleteUser("preview-success@example.com", true);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "matematica-7-ano.png",
                IMAGE_PNG_VALUE,
                validPng()
        );

        mockMvc.perform(multipart("/v1/materiais/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenFor(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status_ia").value("LOW_CONFIDENCE"))
                .andExpect(jsonPath("$.upload_id").isNotEmpty())
                .andExpect(jsonPath("$.best_prediction.titulo.value").value("matematica 7 ano"))
                .andExpect(jsonPath("$.best_prediction.disciplina.value").value("MATEMATICA"));
    }

    @Test
    @DisplayName("POST /api/v1/materiais/preview should bypass Gemini when AI consent is disabled")
    void shouldBypassGeminiWhenConsentIsDisabled() throws Exception {
        Usuario usuario = createCompleteUser("preview-no-consent@example.com", false);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "historia-medio.png",
                IMAGE_PNG_VALUE,
                validPng()
        );

        mockMvc.perform(multipart("/v1/materiais/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenFor(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status_ia").value("FAILURE"))
                .andExpect(jsonPath("$.upload_id").isNotEmpty())
                .andExpect(jsonPath("$.best_prediction").isEmpty())
                .andExpect(jsonPath("$.error_details.missing_fields[0]").value("consentimento_ia"));
    }

    @Test
    @DisplayName("POST /api/v1/materiais/preview should reject unsupported image formats")
    void shouldRejectInvalidMimeType() throws Exception {
        Usuario usuario = createCompleteUser("preview-invalid-image@example.com", true);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "observacoes.txt",
                TEXT_PLAIN_VALUE,
                "arquivo invalido".getBytes()
        );

        mockMvc.perform(multipart("/v1/materiais/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenFor(usuario)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_FORMAT"))
                .andExpect(jsonPath("$.field_errors.image").value("A imagem precisa estar em formato JPEG ou PNG"));
    }

    @Test
    @DisplayName("POST /api/v1/materiais/preview should reject payloads above 5MB")
    void shouldRejectOversizedImage() throws Exception {
        Usuario usuario = createCompleteUser("preview-too-large@example.com", true);
        byte[] oversizedPayload = new byte[(5 * 1024 * 1024) + 32];
        oversizedPayload[0] = (byte) 0xFF;
        oversizedPayload[1] = (byte) 0xD8;
        oversizedPayload[2] = (byte) 0xFF;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "oversized.jpg",
                "image/jpeg",
                oversizedPayload
        );

        mockMvc.perform(multipart("/v1/materiais/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenFor(usuario)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error").value("PAYLOAD_TOO_LARGE"))
                .andExpect(jsonPath("$.message").value("A imagem excede o limite de 5MB"));
    }

    private Usuario createCompleteUser(String email, boolean consentimentoIa) {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email(email)
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Preview User")
                .whatsapp("+5511991234567")
                .cidade("FLORIANOPOLIS")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .consentimentoIa(consentimentoIa)
                .role(Role.USER)
                .build());

        assertThat(usuario.isPerfilCompleto()).isTrue();
        return usuario;
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
                image.setRGB(x, y, 0x00A86B);
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }
}
