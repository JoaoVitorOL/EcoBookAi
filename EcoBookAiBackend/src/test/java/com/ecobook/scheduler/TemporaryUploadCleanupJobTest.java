package com.ecobook.scheduler;

import com.ecobook.BaseIntegrationTest;
import com.ecobook.model.TemporaryUpload;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Role;
import com.ecobook.model.enums.UploadProcessingStatus;
import com.ecobook.repository.TemporaryUploadRepository;
import com.ecobook.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TemporaryUploadCleanupJobTest extends BaseIntegrationTest {

    private static final String SEEDED_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHiPpclJ3DgA6hfa/vw/jemjL5V6cO9e";

    @Autowired
    private TemporaryUploadCleanupJob cleanupJob;

    @Autowired
    private TemporaryUploadRepository temporaryUploadRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("cleanupExpiredUploads should delete expired temporary uploads from disk and database")
    void shouldCleanupExpiredUploads() throws Exception {
        Usuario usuario = createUser("cleanup-job@example.com");
        TemporaryUpload expiredUpload = createUpload(usuario, "expired-upload", LocalDateTime.now().minusHours(2));
        TemporaryUpload activeUpload = createUpload(usuario, "active-upload", LocalDateTime.now().plusHours(2));

        Path expiredPath = Path.of(expiredUpload.getFilePath());
        Path activePath = Path.of(activeUpload.getFilePath());

        cleanupJob.cleanupExpiredUploads();

        assertThat(Files.exists(expiredPath)).isFalse();
        assertThat(Files.exists(activePath)).isTrue();
        assertThat(temporaryUploadRepository.findByUploadId(expiredUpload.getUploadId())).isEmpty();
        assertThat(temporaryUploadRepository.findByUploadId(activeUpload.getUploadId())).isPresent();
    }

    private Usuario createUser(String email) {
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .email(email)
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Cleanup User")
                .whatsapp("+5511991234567")
                .cidade("FLORIANOPOLIS")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .consentimentoIa(true)
                .role(Role.USER)
                .build());
    }

    private TemporaryUpload createUpload(Usuario usuario, String uploadId, LocalDateTime expiresAt) throws Exception {
        Path directory = Path.of("target/test-uploads", usuario.getId().toString(), "temp").toAbsolutePath().normalize();
        Files.createDirectories(directory);

        Path filePath = directory.resolve(uploadId + ".png");
        Files.write(filePath, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

        return temporaryUploadRepository.saveAndFlush(TemporaryUpload.builder()
                .uploadId(uploadId)
                .usuario(usuario)
                .status(UploadProcessingStatus.UPLOADED)
                .filePath(filePath.toString())
                .mimeType("image/png")
                .fileSize(4L)
                .expiresAt(expiresAt)
                .build());
    }
}
