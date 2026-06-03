package com.ecobook.aspect;

import com.ecobook.BaseIntegrationTest;
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

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileCompletenessAspectTest extends BaseIntegrationTest {

    private static final String SEEDED_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHiPpclJ3DgA6hfa/vw/jemjL5V6cO9e";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Protected material endpoints should reject users with incomplete profile")
    void shouldBlockIncompleteProfiles() throws Exception {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("blocked@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Blocked User")
                .perfilCompleto(false)
                .role(Role.USER)
                .build());

        mockMvc.perform(post("/v1/materiais")
                        .contentType(APPLICATION_JSON)
                        .content(validCreatePayload("temp-upload-123"))
                        .header("Authorization", "Bearer " + tokenFor(usuario)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("INCOMPLETE_PROFILE"));
    }

    @Test
    @DisplayName("Protected material endpoints should proceed when the profile is complete")
    void shouldAllowCompleteProfiles() throws Exception {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("allowed@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Allowed User")
                .whatsapp("+5511991234567")
                .cpf("52998224725")
                .cidade("SAO PAULO")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .role(Role.USER)
                .build());

        mockMvc.perform(post("/v1/materiais")
                        .contentType(APPLICATION_JSON)
                        .content(validCreatePayload("temp-upload-123"))
                        .header("Authorization", "Bearer " + tokenFor(usuario)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Material preview skeleton should already be reachable for complete profiles")
    void shouldExposePreviewSkeletonForCompleteProfiles() throws Exception {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("preview@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Preview User")
                .whatsapp("+5511991234567")
                .cpf("52998224725")
                .cidade("SAO PAULO")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .role(Role.USER)
                .build());

        mockMvc.perform(multipart("/v1/materiais/preview")
                        .file(validPreviewFile())
                        .header("Authorization", "Bearer " + tokenFor(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.upload_id").isNotEmpty());
    }

    @Test
    @DisplayName("Material preview should reject users with incomplete profile")
    void shouldBlockIncompleteProfilesFromPreviewEndpoint() throws Exception {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("blocked-preview@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Blocked Preview")
                .perfilCompleto(false)
                .role(Role.USER)
                .build());

        mockMvc.perform(multipart("/v1/materiais/preview")
                        .file(validPreviewFile())
                        .header("Authorization", "Bearer " + tokenFor(usuario)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("INCOMPLETE_PROFILE"));
    }

    @Test
    @DisplayName("Protected material update should reach the controller when the profile is complete")
    void shouldAllowCompleteProfilesToReachUpdateSkeleton() throws Exception {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("update@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Update User")
                .whatsapp("+5511991234567")
                .cpf("52998224725")
                .cidade("SAO PAULO")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .role(Role.USER)
                .build());

        mockMvc.perform(put("/v1/materiais/00000000-0000-0000-0000-000000000123")
                        .contentType(APPLICATION_JSON)
                        .content(validUpdatePayload())
                        .header("Authorization", "Bearer " + tokenFor(usuario)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Protected material delete should reach the controller when the profile is complete")
    void shouldAllowCompleteProfilesToReachDeleteSkeleton() throws Exception {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("delete@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Delete User")
                .whatsapp("+5511991234567")
                .cpf("52998224725")
                .cidade("SAO PAULO")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .role(Role.USER)
                .build());

        mockMvc.perform(delete("/v1/materiais/00000000-0000-0000-0000-000000000123")
                        .header("Authorization", "Bearer " + tokenFor(usuario)))
                .andExpect(status().isNotFound());
    }

    private String tokenFor(Usuario usuario) {
        return jwtTokenProvider.generateToken(
                usuario.getEmail(),
                usuario.getRole().name(),
                usuario.isPerfilCompleto(),
                usuario.getId().toString()
        );
    }

    private String validCreatePayload(String uploadId) {
        return """
                {
                  "upload_id": "%s",
                  "titulo": "Livro de teste",
                  "descricao": "Descricao valida para testar o gate de perfil completo.",
                  "disciplina": "MATEMATICA",
                  "nivel_ensino": "FUNDAMENTAL",
                  "ano": 7,
                  "sistema_ensino": "ANGLO",
                  "estado_conservacao": "BOM",
                  "necessidade_academica": "TEXTBOOKS"
                }
                """.formatted(uploadId);
    }

    private String validUpdatePayload() {
        return """
                {
                  "titulo": "Livro atualizado",
                  "descricao": "Descricao valida para testar o fluxo de atualizacao.",
                  "disciplina": "MATEMATICA",
                  "nivel_ensino": "FUNDAMENTAL",
                  "ano": 7,
                  "sistema_ensino": "ANGLO",
                  "estado_conservacao": "BOM",
                  "necessidade_academica": "TEXTBOOKS"
                }
                """;
    }

    private MockMultipartFile validPreviewFile() throws Exception {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setRGB(x, y, 0x00574B);
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return new MockMultipartFile("file", "preview.png", IMAGE_PNG_VALUE, outputStream.toByteArray());
    }
}
