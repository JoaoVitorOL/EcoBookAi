package com.ecobook.service;

import com.ecobook.exception.BadRequestException;
import com.ecobook.exception.PayloadTooLargeException;
import com.ecobook.model.TemporaryUpload;
import com.ecobook.model.Usuario;
import com.ecobook.repository.TemporaryUploadRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageStorageServiceTest {

    @Mock
    private TemporaryUploadRepository temporaryUploadRepository;

    @Test
    @DisplayName("validateImage should accept payloads exactly at the 5MB limit")
    void shouldAcceptImageExactlyAtFiveMegabytes() throws Exception {
        ImageStorageService service = new ImageStorageService(temporaryUploadRepository);
        ReflectionTestUtils.setField(service, "maxFileSizeMb", 5L);

        byte[] exactBoundaryImage = paddedToExactSize(validPng(), 5 * 1024 * 1024);

        assertThat(service.validateImage(exactBoundaryImage)).isEqualTo("image/png");
    }

    @Test
    @DisplayName("validateImage should reject payloads above the 5MB limit")
    void shouldRejectImageAboveFiveMegabytes() throws Exception {
        ImageStorageService service = new ImageStorageService(temporaryUploadRepository);
        ReflectionTestUtils.setField(service, "maxFileSizeMb", 5L);

        byte[] oversizedImage = paddedToExactSize(validPng(), (5 * 1024 * 1024) + 1);

        assertThatThrownBy(() -> service.validateImage(oversizedImage))
                .isInstanceOf(PayloadTooLargeException.class)
                .hasMessage("A imagem excede 5MB. Escolha um arquivo menor ou recorte a imagem antes de enviar.");
    }

    @Test
    @DisplayName("storeTemporaryImage should delete the primary file when the secondary image is invalid")
    void shouldDeletePrimaryFileWhenSecondaryImageIsInvalid(@TempDir Path tempDir) throws Exception {
        ImageStorageService service = configuredService(tempDir);
        Usuario usuario = usuario();
        MockMultipartFile primaryFile = new MockMultipartFile("file", "front.png", "image/png", validPng());
        MockMultipartFile invalidSecondary = new MockMultipartFile(
                "secondaryFile",
                "back.txt",
                "text/plain",
                "not-an-image".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> service.storeTemporaryImage(usuario, primaryFile, invalidSecondary))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(temporaryUploadRepository);
        assertThat(listRegularFiles(tempDir)).isEmpty();
    }

    @Test
    @DisplayName("storeTemporaryImage should delete created files when persistence fails")
    void shouldDeleteCreatedFilesWhenRepositorySaveFails(@TempDir Path tempDir) throws Exception {
        ImageStorageService service = configuredService(tempDir);
        Usuario usuario = usuario();
        MockMultipartFile primaryFile = new MockMultipartFile("file", "front.png", "image/png", validPng());

        when(temporaryUploadRepository.save(any(TemporaryUpload.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> service.storeTemporaryImage(usuario, primaryFile))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");

        assertThat(listRegularFiles(tempDir)).isEmpty();
    }

    @Test
    @DisplayName("storeTemporaryImage should delete created files on transaction rollback")
    void shouldDeleteCreatedFilesOnTransactionRollback(@TempDir Path tempDir) throws Exception {
        ImageStorageService service = configuredService(tempDir);
        Usuario usuario = usuario();
        MockMultipartFile primaryFile = new MockMultipartFile("file", "front.png", "image/png", validPng());

        when(temporaryUploadRepository.save(any(TemporaryUpload.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.storeTemporaryImage(usuario, primaryFile);

            List<Path> filesBeforeRollback = listRegularFiles(tempDir);
            assertThat(filesBeforeRollback).hasSize(1);
            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }

            assertThat(listRegularFiles(tempDir)).isEmpty();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private ImageStorageService configuredService(Path tempDir) {
        ImageStorageService service = new ImageStorageService(temporaryUploadRepository);
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "maxFileSizeMb", 5L);
        ReflectionTestUtils.setField(service, "servletContextPath", "/api");
        return service;
    }

    private Usuario usuario() {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email("teste@example.com")
                .nome("Teste")
                .build();
    }

    private List<Path> listRegularFiles(Path root) throws Exception {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        }
    }

    private byte[] paddedToExactSize(byte[] baseImage, int targetSize) {
        if (baseImage.length > targetSize) {
            throw new IllegalArgumentException("Base image already exceeds target size");
        }

        byte[] padded = Arrays.copyOf(baseImage, targetSize);
        for (int index = baseImage.length; index < padded.length; index++) {
            padded[index] = 0;
        }
        return padded;
    }

    private byte[] validPng() throws Exception {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setRGB(x, y, 0x00695C);
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }
}
