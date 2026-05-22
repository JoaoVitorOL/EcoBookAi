package com.ecobook.service;

import com.ecobook.exception.BadRequestException;
import com.ecobook.exception.ResourceNotFoundException;
import com.ecobook.model.Material;
import com.ecobook.model.Solicitacao;
import com.ecobook.model.TemporaryUpload;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Role;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.model.enums.StatusSolicitacao;
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
    private final UsuarioRepository usuarioRepository;
    private final SolicitacaoRepository solicitacaoRepository;

    @Transactional(readOnly = true)
    public ImagePayload loadImage(String requesterEmail, String imageId, String side) {
        UUID parsedImageId = parseImageId(imageId);
        boolean backImage = "back".equalsIgnoreCase(side);

        Usuario requester = usuarioRepository.findByEmailIgnoreCase(requesterEmail)
                .orElseThrow(() -> new AccessDeniedException("Usuário autenticado não encontrado"));

        TemporaryUpload upload = temporaryUploadRepository.findById(parsedImageId)
                .orElseThrow(() -> new ResourceNotFoundException("Imagem não encontrada"));

        Material material = upload.getMaterial();
        if (material == null) {
            throw new ResourceNotFoundException("Imagem não encontrada");
        }

        if (!canAccess(requester, material, backImage)) {
            throw new AccessDeniedException("Você não tem permissão para acessar esta imagem");
        }

        String filePath = backImage ? upload.getSecondaryFilePath() : upload.getFilePath();
        String mimeType = backImage ? upload.getSecondaryMimeType() : upload.getMimeType();
        if (!StringUtils.hasText(filePath)) {
            throw new ResourceNotFoundException("Imagem não encontrada");
        }

        Path path = Path.of(filePath).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Imagem não encontrada");
        }

        return new ImagePayload(new FileSystemResource(path), resolveMimeType(mimeType, path));
    }

    private boolean canAccess(Usuario requester, Material material, boolean backImage) {
        if (requester.getRole() == Role.ADMIN) {
            return true;
        }

        Usuario owner = material.getDoador();
        if (owner != null && owner.getId() != null && owner.getId().equals(requester.getId())) {
            return true;
        }

        if (!backImage && material.getStatus() == StatusMaterial.DISPONIVEL) {
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
            throw new BadRequestException("Identificador de imagem inválido", Map.of(
                    "image_id", "Informe um UUID válido"
            ));
        }

        try {
            return UUID.fromString(imageId.trim());
        } catch (IllegalArgumentException exception) {
            LinkedHashMap<String, String> fieldErrors = new LinkedHashMap<>();
            fieldErrors.put("image_id", "Informe um UUID válido");
            throw new BadRequestException("Identificador de imagem inválido", fieldErrors);
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

    public record ImagePayload(Resource resource, String contentType) {
    }
}
