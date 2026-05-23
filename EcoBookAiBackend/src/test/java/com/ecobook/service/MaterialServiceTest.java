package com.ecobook.service;

import com.ecobook.dto.CreateMaterialRequestDTO;
import com.ecobook.exception.ResourceNotFoundException;
import com.ecobook.model.TemporaryUpload;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Role;
import com.ecobook.model.enums.UploadProcessingStatus;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.SolicitacaoRepository;
import com.ecobook.repository.TemporaryUploadRepository;
import com.ecobook.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MaterialServiceTest {

    @TempDir
    Path tempDir;

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final MaterialRepository materialRepository = mock(MaterialRepository.class);
    private final TemporaryUploadRepository temporaryUploadRepository = mock(TemporaryUploadRepository.class);
    private final SolicitacaoRepository solicitacaoRepository = mock(SolicitacaoRepository.class);
    private final ImageStorageService imageStorageService = mock(ImageStorageService.class);
    private final GeminiService geminiService = mock(GeminiService.class);
    private final MaterialMapper materialMapper = new MaterialMapper();
    private final NotificationPayloadFactory notificationPayloadFactory = new NotificationPayloadFactory();
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private final MaterialService materialService = new MaterialService(
            usuarioRepository,
            materialRepository,
            temporaryUploadRepository,
            solicitacaoRepository,
            imageStorageService,
            geminiService,
            materialMapper,
            notificationPayloadFactory,
            eventPublisher
    );

    @Test
    @DisplayName("createMaterial should remove promoted files when persistence fails after image promotion")
    void shouldCleanupPromotedFilesWhenSaveFails() throws IOException {
        Usuario usuario = sampleUser();
        TemporaryUpload upload = sampleUpload(usuario);
        CreateMaterialRequestDTO request = sampleRequest(upload.getUploadId());
        Path promotedFront = Path.of("target/test-uploads/promoted-front.jpg");
        Path promotedBack = Path.of("target/test-uploads/promoted-back.jpg");

        when(usuarioRepository.findByEmailIgnoreCase(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(temporaryUploadRepository.findByUploadId(upload.getUploadId())).thenReturn(Optional.of(upload));
        when(imageStorageService.promoteTemporaryImage(upload))
                .thenReturn(new ImageStorageService.PromotedImage(promotedFront, "/api/uploads/front.jpg"));
        when(imageStorageService.promoteSecondaryTemporaryImage(upload))
                .thenReturn(new ImageStorageService.PromotedImage(promotedBack, "/api/uploads/back.jpg"));
        when(materialRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("falha simulada no banco"));

        assertThatThrownBy(() -> materialService.createMaterial(usuario.getEmail(), request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("falha simulada no banco");

        verify(imageStorageService).deleteIfExists(promotedFront.toString());
        verify(imageStorageService).deleteIfExists(promotedBack.toString());
        verify(temporaryUploadRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("createMaterial should remove the already promoted front image when the back promotion fails")
    void shouldCleanupFrontImageWhenBackPromotionFails() throws IOException {
        Usuario usuario = sampleUser();
        TemporaryUpload upload = sampleUpload(usuario);
        CreateMaterialRequestDTO request = sampleRequest(upload.getUploadId());
        Path promotedFront = Path.of("target/test-uploads/promoted-front.jpg");

        when(usuarioRepository.findByEmailIgnoreCase(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(temporaryUploadRepository.findByUploadId(upload.getUploadId())).thenReturn(Optional.of(upload));
        when(imageStorageService.promoteTemporaryImage(upload))
                .thenReturn(new ImageStorageService.PromotedImage(promotedFront, "/api/uploads/front.jpg"));
        when(imageStorageService.promoteSecondaryTemporaryImage(upload))
                .thenThrow(new ResourceNotFoundException("falha simulada ao promover verso"));

        assertThatThrownBy(() -> materialService.createMaterial(usuario.getEmail(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("falha simulada ao promover verso");

        verify(imageStorageService).deleteIfExists(promotedFront.toString());
    }

    private Usuario sampleUser() {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email("material-service-test@example.com")
                .passwordHash("hash")
                .nome("Doador Teste")
                .whatsapp("+5511991234567")
                .cidade("FLORIANOPOLIS")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .consentimentoIa(true)
                .role(Role.USER)
                .build();
    }

    private TemporaryUpload sampleUpload(Usuario usuario) throws IOException {
        Path frontFile = Files.write(tempDir.resolve("temp-front.jpg"), new byte[]{1, 2, 3});
        Path backFile = Files.write(tempDir.resolve("temp-back.jpg"), new byte[]{4, 5, 6});
        return TemporaryUpload.builder()
                .id(UUID.randomUUID())
                .uploadId("temp-upload-test")
                .usuario(usuario)
                .status(UploadProcessingStatus.PREVIEW_SUCCESS)
                .filePath(frontFile.toString())
                .secondaryFilePath(backFile.toString())
                .mimeType("image/jpeg")
                .secondaryMimeType("image/jpeg")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    private CreateMaterialRequestDTO sampleRequest(String uploadId) {
        return CreateMaterialRequestDTO.builder()
                .uploadId(uploadId)
                .titulo("Colecao de Algebra")
                .autor("Autor Exemplo")
                .editora("Editora Exemplo")
                .descricao("Descricao suficiente para o teste")
                .disciplina("MATEMATICA")
                .nivelEnsino("FUNDAMENTAL")
                .ano(7)
                .sistemaEnsino("ANGLO")
                .estadoConservacao("BOM")
                .dataPublicacao(2024)
                .build();
    }
}
