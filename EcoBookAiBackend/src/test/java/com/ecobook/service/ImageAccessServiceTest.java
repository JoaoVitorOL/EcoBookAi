package com.ecobook.service;

import com.ecobook.exception.ResourceNotFoundException;
import com.ecobook.model.Material;
import com.ecobook.model.TemporaryUpload;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Role;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.SolicitacaoRepository;
import com.ecobook.repository.TemporaryUploadRepository;
import com.ecobook.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageAccessServiceTest {

    @TempDir
    Path tempDir;

    private final TemporaryUploadRepository temporaryUploadRepository = mock(TemporaryUploadRepository.class);
    private final MaterialRepository materialRepository = mock(MaterialRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final SolicitacaoRepository solicitacaoRepository = mock(SolicitacaoRepository.class);
    private final ImageStorageService imageStorageService = mock(ImageStorageService.class);
    private final ImageAccessService imageAccessService = new ImageAccessService(
            temporaryUploadRepository,
            materialRepository,
            usuarioRepository,
            solicitacaoRepository,
            imageStorageService
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

        Path file = Files.createFile(tempDir.resolve("front.jpg"));
        Files.writeString(file, "content");

        TemporaryUpload upload = TemporaryUpload.builder()
                .id(UUID.randomUUID())
                .material(material)
                .filePath(file.toString())
                .mimeType("image/jpeg")
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(requester));
        when(temporaryUploadRepository.findById(upload.getId())).thenReturn(Optional.of(upload));

        ImageAccessService.ImagePayload payload = imageAccessService.loadImage(
                "student@example.com",
                upload.getId().toString(),
                "front"
        );

        assertThat(payload.contentType()).isEqualTo("image/jpeg");
        assertThat(payload.resource().exists()).isTrue();
    }

    @Test
    @DisplayName("loadImage should fall back to promoted back image when tracking metadata is unavailable")
    void shouldResolvePromotedBackImageWithoutTrackingMetadata() throws Exception {
        UUID uploadTrackingId = UUID.randomUUID();
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
                .uploadId("temp-upload-123")
                .uploadTrackingId(uploadTrackingId)
                .build();

        Path backImage = Files.createFile(tempDir.resolve("temp-upload-123-back.png"));

        when(usuarioRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(requester));
        when(temporaryUploadRepository.findById(uploadTrackingId)).thenReturn(Optional.empty());
        when(materialRepository.findByUploadTrackingId(uploadTrackingId)).thenReturn(Optional.of(material));
        when(imageStorageService.findPromotedImagePath(donor.getId(), "temp-upload-123", true)).thenReturn(backImage);

        ImageAccessService.ImagePayload payload = imageAccessService.loadImage(
                "student@example.com",
                uploadTrackingId.toString(),
                "back"
        );

        assertThat(payload.contentType()).isEqualTo("image/png");
        assertThat(payload.resource().exists()).isTrue();
    }

    @Test
    @DisplayName("loadImage should fall back to the stored public upload url for legacy materials")
    void shouldResolveLegacyStoredUploadUrl() throws Exception {
        UUID uploadTrackingId = UUID.randomUUID();
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

        Path uploadRoot = tempDir.resolve("uploads");
        Path donorDirectory = uploadRoot.resolve(donor.getId().toString());
        Files.createDirectories(donorDirectory);
        Path storedFile = Files.createFile(donorDirectory.resolve("legacy-front.jpg"));

        Material material = Material.builder()
                .id(UUID.randomUUID())
                .doador(donor)
                .titulo("Livro legado")
                .status(StatusMaterial.DISPONIVEL)
                .uploadTrackingId(uploadTrackingId)
                .imagemUrl("/uploads/" + donor.getId() + "/legacy-front.jpg")
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(requester));
        when(temporaryUploadRepository.findById(uploadTrackingId)).thenReturn(Optional.empty());
        when(materialRepository.findByUploadTrackingId(uploadTrackingId)).thenReturn(Optional.of(material));
        when(imageStorageService.getUploadDir()).thenReturn(uploadRoot.toString());

        ImageAccessService.ImagePayload payload = imageAccessService.loadImage(
                "student@example.com",
                uploadTrackingId.toString(),
                "front"
        );

        assertThat(payload.contentType()).isEqualTo("image/jpeg");
        assertThat(payload.resource().exists()).isTrue();
        assertThat(payload.resource().getFile().toPath()).isEqualTo(storedFile);
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
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
