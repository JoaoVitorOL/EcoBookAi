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

    public StoredTemporaryUpload storeTemporaryImage(Usuario usuario, MultipartFile file) {
        byte[] imageBytes = readFileBytes(file);
        String mimeType = validateImage(imageBytes);
        String uploadId = "temp-upload-" + UUID.randomUUID();
        String extension = extensionForMimeType(mimeType);

        try {
            Path directory = Path.of(uploadDir, usuario.getId().toString(), "temp").toAbsolutePath().normalize();
            Files.createDirectories(directory);

            Path filePath = directory.resolve(uploadId + extension);
            Files.write(filePath, imageBytes);

            TemporaryUpload upload = temporaryUploadRepository.save(TemporaryUpload.builder()
                    .uploadId(uploadId)
                    .usuario(usuario)
                    .status(UploadProcessingStatus.UPLOADED)
                    .filePath(filePath.toString())
                    .mimeType(mimeType)
                    .fileSize((long) imageBytes.length)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build());

            return new StoredTemporaryUpload(upload, imageBytes, mimeType, file.getOriginalFilename());
        } catch (IOException ex) {
            throw new ResourceNotFoundException("Nao foi possivel armazenar a imagem temporaria", ex);
        }
    }

    public String validateImage(byte[] imageBytes) {
        if (imageBytes.length == 0) {
            throw invalidImage("image", "Envie uma imagem JPEG ou PNG valida");
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
                throw invalidImage("image", "Nao foi possivel decodificar a imagem enviada");
            }
        } catch (IOException ex) {
            throw invalidImage("image", "Nao foi possivel decodificar a imagem enviada");
        }

        return mimeType;
    }

    public PromotedImage promoteTemporaryImage(TemporaryUpload upload) {
        Path tempPath = resolvePath(upload.getFilePath());
        if (!Files.exists(tempPath)) {
            throw new ResourceNotFoundException("Temporary upload not found or expired");
        }

        try {
            Path destinationDirectory = Path.of(uploadDir, upload.getUsuario().getId().toString())
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(destinationDirectory);

            Path destination = destinationDirectory.resolve(tempPath.getFileName().toString());
            Files.move(tempPath, destination, StandardCopyOption.REPLACE_EXISTING);
            String publicUrl = "/api/uploads/" + upload.getUsuario().getId() + "/" + destination.getFileName();
            return new PromotedImage(destination, publicUrl);
        } catch (IOException ex) {
            throw new ResourceNotFoundException("Nao foi possivel promover a imagem para armazenamento permanente", ex);
        }
    }

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
            String originalFilename
    ) {
    }

    public record PromotedImage(Path absolutePath, String publicUrl) {
    }

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw invalidImage("image", "Nao foi possivel ler a imagem enviada");
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

    private BadRequestException invalidImage(String field, String message) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(field, message);
        return new BadRequestException("Imagem invalida", fieldErrors);
    }

    private Path resolvePath(String filePath) {
        return Path.of(filePath).toAbsolutePath().normalize();
    }
}
