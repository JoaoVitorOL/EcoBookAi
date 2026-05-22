package com.ecobook.service;

import com.ecobook.exception.ResourceNotFoundException;
import com.ecobook.model.Material;
import com.ecobook.model.TemporaryUpload;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Role;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.repository.SolicitacaoRepository;
import com.ecobook.repository.TemporaryUploadRepository;
import com.ecobook.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageAccessServiceTest {

    private final TemporaryUploadRepository temporaryUploadRepository = mock(TemporaryUploadRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final SolicitacaoRepository solicitacaoRepository = mock(SolicitacaoRepository.class);
    private final ImageAccessService imageAccessService = new ImageAccessService(
            temporaryUploadRepository,
            usuarioRepository,
            solicitacaoRepository
    );

    @Test
    @DisplayName("loadImage should allow authenticated users to access front image of available material")
    void shouldAllowFrontImageForDiscovery() throws Exception {
        Usuario requester = Usuario.builder()
                .id(UUID.randomUUID())
                .email("student@example.com")
                .passwordHash("hash")
                .nome("Estudante")
                .role(Role.USER)
                .build();
        Usuario donor = Usuario.builder()
                .id(UUID.randomUUID())
                .email("donor@example.com")
                .passwordHash("hash")
                .nome("Doador")
                .role(Role.USER)
                .build();
        Material material = Material.builder()
                .id(UUID.randomUUID())
                .doador(donor)
                .titulo("Livro")
                .status(StatusMaterial.DISPONIVEL)
                .build();

        Path file = Files.createTempFile("ecobook-image-front", ".jpg");
        Files.writeString(file, "content");

        TemporaryUpload upload = TemporaryUpload.builder()
                .id(UUID.randomUUID())
                .material(material)
                .filePath(file.toString())
                .mimeType("image/jpeg")
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(requester));
        when(temporaryUploadRepository.findById(upload.getId())).thenReturn(Optional.of(upload));

        ImageAccessService.ImagePayload payload = imageAccessService.loadImage("student@example.com", upload.getId().toString(), "front");

        assertThat(payload.contentType()).isEqualTo("image/jpeg");
        assertThat(payload.resource().exists()).isTrue();
    }

    @Test
    @DisplayName("loadImage should reject missing files")
    void shouldRejectMissingFiles() {
        Usuario requester = Usuario.builder()
                .id(UUID.randomUUID())
                .email("student@example.com")
                .passwordHash("hash")
                .nome("Estudante")
                .role(Role.USER)
                .build();
        Material material = Material.builder()
                .id(UUID.randomUUID())
                .doador(requester)
                .titulo("Livro")
                .status(StatusMaterial.DISPONIVEL)
                .build();

        TemporaryUpload upload = TemporaryUpload.builder()
                .id(UUID.randomUUID())
                .material(material)
                .filePath("C:/tmp/inexistente-front.jpg")
                .mimeType("image/jpeg")
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(requester));
        when(temporaryUploadRepository.findById(upload.getId())).thenReturn(Optional.of(upload));
        assertThatThrownBy(() -> imageAccessService.loadImage("student@example.com", upload.getId().toString(), "front"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Imagem não encontrada");
    }
}
