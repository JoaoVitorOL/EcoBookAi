package com.ecobook.service;

import com.ecobook.exception.BadRequestException;
import com.ecobook.exception.ResourceNotFoundException;
import com.ecobook.model.Material;
import com.ecobook.model.TemporaryUpload;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Role;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.model.enums.StatusSolicitacao;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.SolicitacaoRepository;
import com.ecobook.repository.TemporaryUploadRepository;
import com.ecobook.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageAccessService {

    private static final Set<StatusSolicitacao> AUTHORIZED_REQUEST_STATUSES = Set.of(
            StatusSolicitacao.APROVADA,
            StatusSolicitacao.CONCLUIDA
    );

    private final TemporaryUploadRepository temporaryUploadRepository;
    private final MaterialRepository materialRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final ImageStorageService imageStorageService;

    /**
     * Loads an image payload after applying requester access checks.
     * @param requesterEmail email of the user requesting image access
     * @param imageId upload tracking identifier for the image
     * @param side requested image side selector
     * @return loaded value
     */
    @Transactional(readOnly = true)
    public ImagePayload loadImage(String requesterEmail, String imageId, String side) {
        UUID parsedImageId = parseImageId(imageId);
        boolean backImage = "back".equalsIgnoreCase(side);

        Usuario requester = usuarioRepository.findByEmailIgnoreCase(requesterEmail)
                .orElseThrow(() -> new AccessDeniedException("Usuario autenticado nao encontrado"));

        TemporaryUpload upload = temporaryUploadRepository.findById(parsedImageId).orElse(null);
        Material material = upload != null
                ? upload.getMaterial()
                : materialRepository.findByUploadTrackingId(parsedImageId).orElse(null);
        if (material == null) {
            throw new ResourceNotFoundException("Imagem nao encontrada");
        }

        if (!canAccess(requester, material)) {
            throw new AccessDeniedException("Voce nao tem permissao para acessar esta imagem");
        }

        Path path = resolveImagePath(upload, material, backImage);
        if (path == null || !Files.exists(path)) {
            throw new ResourceNotFoundException("Imagem nao encontrada");
        }

        String mimeType = upload == null
                ? null
                : backImage ? upload.getSecondaryMimeType() : upload.getMimeType();
        return new ImagePayload(new FileSystemResource(path), resolveMimeType(mimeType, path));
    }

    private boolean canAccess(Usuario requester, Material material) {
        if (requester.getRole() == Role.ADMIN) {
            return true;
        }

        Usuario owner = material.getDoador();
        if (owner != null && owner.getId() != null && owner.getId().equals(requester.getId())) {
            return true;
        }

        if (material.getStatus() == StatusMaterial.DISPONIVEL) {
            return true;
        }

        return solicitacaoRepository.existsByMaterialIdAndEstudanteIdAndStatusIn(
                material.getId(),
                requester.getId(),
                AUTHORIZED_REQUEST_STATUSES
        );
    }

    private UUID parseImageId(String imageId) {
        if (!StringUtils.hasText(imageId)) {
            throw new BadRequestException("Identificador de imagem invalido", Map.of(
                    "image_id", "Informe um UUID valido"
            ));
        }

        try {
            return UUID.fromString(imageId.trim());
        } catch (IllegalArgumentException exception) {
            LinkedHashMap<String, String> fieldErrors = new LinkedHashMap<>();
            fieldErrors.put("image_id", "Informe um UUID valido");
            throw new BadRequestException("Identificador de imagem invalido", fieldErrors);
        }
    }

    private String resolveMimeType(String declaredMimeType, Path path) {
        if (StringUtils.hasText(declaredMimeType)) {
            return declaredMimeType.trim();
        }

        String fileName = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        return "image/jpeg";
    }

    private Path resolveImagePath(TemporaryUpload upload, Material material, boolean backImage) {
        Path trackedPath = resolveTrackedPath(upload, backImage);
        if (trackedPath != null) {
            return trackedPath;
        }

        Usuario owner = material.getDoador();
        if (owner != null && owner.getId() != null) {
            Path promotedPath = imageStorageService.findPromotedImagePath(
                    owner.getId(),
                    material.getUploadId(),
                    backImage
            );
            if (promotedPath != null) {
                return promotedPath;
            }
        }

        String storedUrl = backImage ? material.getImagemVersoUrl() : material.getImagemUrl();
        return resolvePathFromStoredUrl(storedUrl);
    }

    private Path resolveTrackedPath(TemporaryUpload upload, boolean backImage) {
        if (upload == null) {
            return null;
        }

        String filePath = backImage ? upload.getSecondaryFilePath() : upload.getFilePath();
        if (!StringUtils.hasText(filePath)) {
            return null;
        }

        return Path.of(filePath).toAbsolutePath().normalize();
    }

    private Path resolvePathFromStoredUrl(String storedUrl) {
        if (!StringUtils.hasText(storedUrl)) {
            return null;
        }

        String normalized = storedUrl.trim();
        int uploadsIndex = normalized.indexOf("/uploads/");
        if (uploadsIndex < 0) {
            return null;
        }

        String relativePath = normalized.substring(uploadsIndex + "/uploads/".length())
                .split("\\?", 2)[0]
                .replaceFirst("^/+", "");
        if (!StringUtils.hasText(relativePath)) {
            return null;
        }

        Path uploadRoot = Path.of(imageStorageService.getUploadDir()).toAbsolutePath().normalize();
        Path resolvedPath = uploadRoot.resolve(relativePath).normalize();
        if (!resolvedPath.startsWith(uploadRoot)) {
            return null;
        }
        return resolvedPath;
    }

    public record ImagePayload(Resource resource, String contentType) {
    }
}
