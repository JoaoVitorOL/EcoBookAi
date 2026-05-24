package com.ecobook.service;

import com.ecobook.exception.BadRequestException;
import com.ecobook.exception.PayloadTooLargeException;
import com.ecobook.exception.ResourceNotFoundException;
import com.ecobook.model.TemporaryUpload;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.UploadProcessingStatus;
import com.ecobook.repository.TemporaryUploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageStorageService {

    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};

    private final TemporaryUploadRepository temporaryUploadRepository;

    @Value("${storage.upload-dir}")
    private String uploadDir;

    @Value("${storage.max-file-size-mb:5}")
    private long maxFileSizeMb;

    @Value("${server.servlet.context-path:}")
    private String servletContextPath;

    /**
     * Stores an uploaded image pair as a temporary preview artifact.
     * @param usuario user entity involved in the operation
     * @param file primary uploaded image file
     * @return result of the operation
     */
    public StoredTemporaryUpload storeTemporaryImage(Usuario usuario, MultipartFile file) {
        return storeTemporaryImage(usuario, file, null);
    }

    public StoredTemporaryUpload storeTemporaryImage(Usuario usuario,
                                                     MultipartFile file,
                                                     MultipartFile secondaryFile) {
        byte[] imageBytes = readFileBytes(file);
        String mimeType = validateImage(imageBytes);
        byte[] secondaryImageBytes = null;
        String secondaryMimeType = null;
        String uploadId = "temp-upload-" + UUID.randomUUID();

        try {
            Path directory = Path.of(uploadDir, usuario.getId().toString(), "temp").toAbsolutePath().normalize();
            Files.createDirectories(directory);

            Path filePath = storeImageBytes(directory, uploadId, imageBytes, mimeType);

            TemporaryUpload.TemporaryUploadBuilder uploadBuilder = TemporaryUpload.builder()
                    .uploadId(uploadId)
                    .usuario(usuario)
                    .status(UploadProcessingStatus.UPLOADED)
                    .filePath(filePath.toString())
                    .mimeType(mimeType)
                    .fileSize((long) imageBytes.length)
                    .expiresAt(LocalDateTime.now().plusHours(24));

            if (secondaryFile != null && !secondaryFile.isEmpty()) {
                secondaryImageBytes = readFileBytes(secondaryFile);
                secondaryMimeType = validateImage(secondaryImageBytes);
                Path secondaryFilePath = storeImageBytes(directory, uploadId + "-back", secondaryImageBytes, secondaryMimeType);
                uploadBuilder
                        .secondaryFilePath(secondaryFilePath.toString())
                        .secondaryMimeType(secondaryMimeType)
                        .secondaryFileSize((long) secondaryImageBytes.length);
            }

            TemporaryUpload upload = temporaryUploadRepository.save(uploadBuilder.build());
            return new StoredTemporaryUpload(
                    upload,
                    imageBytes,
                    mimeType,
                    file.getOriginalFilename(),
                    secondaryImageBytes,
                    secondaryMimeType
            );
        } catch (IOException ex) {
            throw new ResourceNotFoundException("Não foi possível armazenar a imagem temporária", ex);
        }
    }

    /**
     * Validates uploaded image bytes and returns the normalized MIME type.
     * @param imageBytes image bytes to validate or classify
     * @return result of the operation
     */
    public String validateImage(byte[] imageBytes) {
        if (imageBytes.length == 0) {
            throw invalidImage("image", "Envie uma imagem JPEG ou PNG válida");
        }

        if (imageBytes.length > maxFileSizeBytes()) {
            throw new PayloadTooLargeException("A imagem excede o limite de 5MB");
        }

        String mimeType = detectMimeType(imageBytes);
        if (mimeType == null) {
            throw invalidImage("image", "A imagem precisa estar em formato JPEG ou PNG");
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw invalidImage("image", "Não foi possível decodificar a imagem enviada");
            }
        } catch (IOException ex) {
            throw invalidImage("image", "Não foi possível decodificar a imagem enviada");
        }

        return mimeType;
    }

    /**
     * Promotes the primary temporary image into durable storage.
     * @param upload temporary upload record to promote or inspect
     * @return result of the operation
     */
    public PromotedImage promoteTemporaryImage(TemporaryUpload upload) {
        return promoteStoredImage(upload.getUsuario().getId(), upload.getFilePath());
    }

    /**
     * Promotes the secondary temporary image into durable storage when present.
     * @param upload temporary upload record to promote or inspect
     * @return result of the operation
     */
    public PromotedImage promoteSecondaryTemporaryImage(TemporaryUpload upload) {
        if (!org.springframework.util.StringUtils.hasText(upload.getSecondaryFilePath())) {
            return null;
        }
        return promoteStoredImage(upload.getUsuario().getId(), upload.getSecondaryFilePath());
    }

    private PromotedImage promoteStoredImage(UUID userId, String storedFilePath) {
        Path tempPath = resolvePath(storedFilePath);
        if (!Files.exists(tempPath)) {
            throw new ResourceNotFoundException("Temporary upload not found or expired");
        }

        try {
            Path destinationDirectory = Path.of(uploadDir, userId.toString())
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(destinationDirectory);

            Path destination = destinationDirectory.resolve(tempPath.getFileName().toString());
            Files.move(tempPath, destination, StandardCopyOption.REPLACE_EXISTING);
            String publicUrl = buildPublicUrl(userId, destination.getFileName().toString());
            return new PromotedImage(destination, publicUrl);
        } catch (IOException ex) {
            throw new ResourceNotFoundException("Não foi possível promover a imagem para armazenamento permanente", ex);
        }
    }

    /**
     * Deletes a stored file when the target path exists.
     * @param filePath file path to delete when present
     */
    public void deleteIfExists(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(resolvePath(filePath));
        } catch (IOException ex) {
            log.warn("Nao foi possivel remover arquivo temporario {}", filePath, ex);
        }
    }

    public record StoredTemporaryUpload(
            TemporaryUpload upload,
            byte[] imageBytes,
            String mimeType,
            String originalFilename,
            byte[] secondaryImageBytes,
            String secondaryMimeType
    ) {
    }

    public record PromotedImage(Path absolutePath, String publicUrl) {
    }

    /**
     * Returns the absolute upload directory configured for stored files.
     * @return requested value
     */
    public String getUploadDir() {
        return uploadDir;
    }

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw invalidImage("image", "Não foi possível ler a imagem enviada");
        }
    }

    private long maxFileSizeBytes() {
        return maxFileSizeMb * 1024 * 1024;
    }

    private String detectMimeType(byte[] imageBytes) {
        if (startsWith(imageBytes, JPEG_MAGIC)) {
            return "image/jpeg";
        }
        if (startsWith(imageBytes, PNG_MAGIC)) {
            return "image/png";
        }
        return null;
    }

    private boolean startsWith(byte[] input, byte[] prefix) {
        if (input.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (input[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private String extensionForMimeType(String mimeType) {
        return "image/png".equals(mimeType) ? ".png" : ".jpg";
    }

    private Path storeImageBytes(Path directory, String uploadId, byte[] imageBytes, String mimeType) throws IOException {
        Path filePath = directory.resolve(uploadId + extensionForMimeType(mimeType));
        Files.write(filePath, imageBytes);
        return filePath;
    }

    private BadRequestException invalidImage(String field, String message) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(field, message);
        return new BadRequestException("Imagem inválida", fieldErrors);
    }

    private Path resolvePath(String filePath) {
        return Path.of(filePath).toAbsolutePath().normalize();
    }

    private String buildPublicUrl(UUID userId, String fileName) {
        String contextPath = normalizeContextPath();
        return contextPath + "/uploads/" + userId + "/" + fileName;
    }

    private String normalizeContextPath() {
        if (servletContextPath == null || servletContextPath.isBlank() || "/".equals(servletContextPath.trim())) {
            return "";
        }

        String normalized = servletContextPath.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
