package com.ecobook.service;

import com.ecobook.dto.CreateMaterialRequestDTO;
import com.ecobook.dto.GeminiResponseDTO;
import com.ecobook.dto.MaterialDTO;
import com.ecobook.dto.UpdateMaterialRequestDTO;
import com.ecobook.exception.BadRequestException;
import com.ecobook.exception.ConflictException;
import com.ecobook.exception.ResourceNotFoundException;
import com.ecobook.exception.UnprocessableEntityException;
import com.ecobook.model.Material;
import com.ecobook.model.TemporaryUpload;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Disciplina;
import com.ecobook.model.enums.EstadoConservacao;
import com.ecobook.model.enums.NivelEnsino;
import com.ecobook.model.enums.SistemaEnsino;
import com.ecobook.model.enums.StatusIA;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.model.enums.UploadProcessingStatus;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.TemporaryUploadRepository;
import com.ecobook.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialService {

    private final UsuarioRepository usuarioRepository;
    private final MaterialRepository materialRepository;
    private final TemporaryUploadRepository temporaryUploadRepository;
    private final ImageStorageService imageStorageService;
    private final GeminiService geminiService;
    private final MaterialMapper materialMapper;

    @Transactional
    public GeminiResponseDTO previewMaterial(String email, MultipartFile file) {
        Usuario usuario = loadUsuario(email);
        ImageStorageService.StoredTemporaryUpload storedUpload = imageStorageService.storeTemporaryImage(usuario, file);

        if (!Boolean.TRUE.equals(usuario.getConsentimentoIa())) {
            TemporaryUpload upload = storedUpload.upload();
            upload.setStatus(UploadProcessingStatus.PREVIEW_FAILURE);
            upload.setStatusIa(StatusIA.FAILURE);
            upload.setConfiancaIa(null);
            temporaryUploadRepository.save(upload);

            GeminiResponseDTO response = geminiService.failureResponse(
                    "Consentimento de IA desabilitado; siga com preenchimento manual.",
                    false,
                    false,
                    java.util.List.of("consentimento_ia"),
                    java.util.List.of()
            );
            response.setUploadId(upload.getUploadId());
            return response;
        }

        GeminiResponseDTO response = geminiService.classifyMaterial(
                storedUpload.originalFilename(),
                storedUpload.imageBytes(),
                storedUpload.mimeType()
        );
        response.setUploadId(storedUpload.upload().getUploadId());

        updatePreviewTracking(storedUpload.upload(), response);
        return response;
    }

    @Transactional
    public MaterialDTO createMaterial(String email, CreateMaterialRequestDTO request) {
        Usuario usuario = loadUsuario(email);
        ValidatedMaterialRequest validated = validateCreateRequest(request);

        TemporaryUpload upload = temporaryUploadRepository.findByUploadId(validated.uploadId())
                .orElseThrow(() -> new ResourceNotFoundException("Temporary upload not found or expired"));

        if (upload.getMaterial() != null) {
            throw new ConflictException("Este upload_id ja foi utilizado");
        }

        if (upload.getUsuario() == null || !upload.getUsuario().getId().equals(usuario.getId())) {
            throw new ResourceNotFoundException("Temporary upload not found or expired");
        }

        if (upload.getExpiresAt() != null && upload.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("Temporary upload not found or expired");
        }

        if (!StringUtils.hasText(upload.getFilePath()) || !Files.exists(Path.of(upload.getFilePath()))) {
            throw new ResourceNotFoundException("Temporary upload not found or expired");
        }

        ImageStorageService.PromotedImage promotedImage = imageStorageService.promoteTemporaryImage(upload);

        try {
            Material material = materialRepository.save(Material.builder()
                    .doador(usuario)
                    .titulo(validated.data().titulo())
                    .autor(validated.data().autor())
                    .editora(validated.data().editora())
                    .descricao(validated.data().descricao())
                    .disciplina(validated.data().disciplina())
                    .nivelEnsino(validated.data().nivelEnsino())
                    .ano(validated.data().ano())
                    .sistemaEnsino(validated.data().sistemaEnsino())
                    .estadoConservacao(validated.data().estadoConservacao())
                    .status(StatusMaterial.DISPONIVEL)
                    .imagemUrl(promotedImage.publicUrl())
                    .uploadId(validated.uploadId())
                    .cidade(usuario.getCidade())
                    .bairro(usuario.getBairro())
                    .dataPublicacao(validated.data().dataPublicacao())
                    .statusIa(upload.getStatusIa())
                    .confiancaIa(upload.getConfiancaIa())
                    .build());

            upload.setMaterial(material);
            upload.setStatus(UploadProcessingStatus.MATERIAL_CREATED);
            upload.setExpiresAt(null);
            upload.setFilePath(promotedImage.absolutePath().toString());
            temporaryUploadRepository.save(upload);

            return materialMapper.toDto(material);
        } catch (RuntimeException ex) {
            log.error("Falha ao persistir material para upload {}", validated.uploadId(), ex);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<MaterialDTO> listCurrentUserMaterials(String email) {
        Usuario usuario = loadUsuario(email);
        return materialRepository.findByDoadorIdOrderByCriadoEmDesc(usuario.getId()).stream()
                .map(materialMapper::toDto)
                .toList();
    }

    @Transactional
    public MaterialDTO updateMaterial(String email, String materialId, UpdateMaterialRequestDTO request) {
        Usuario usuario = loadUsuario(email);
        Material material = loadOwnedMaterial(materialId, usuario);
        ensureStatusAllowsEditing(material);

        ValidatedMaterialData validated = validateUpdateRequest(request);
        material.setTitulo(validated.titulo());
        material.setAutor(validated.autor());
        material.setEditora(validated.editora());
        material.setDescricao(validated.descricao());
        material.setDisciplina(validated.disciplina());
        material.setNivelEnsino(validated.nivelEnsino());
        material.setAno(validated.ano());
        material.setSistemaEnsino(validated.sistemaEnsino());
        material.setEstadoConservacao(validated.estadoConservacao());
        material.setDataPublicacao(validated.dataPublicacao());

        return materialMapper.toDto(materialRepository.save(material));
    }

    @Transactional
    public void cancelMaterial(String email, String materialId) {
        Usuario usuario = loadUsuario(email);
        Material material = loadOwnedMaterial(materialId, usuario);
        ensureStatusAllowsCancellation(material);
        material.setStatus(StatusMaterial.CANCELADO);
        materialRepository.save(material);
    }

    private Usuario loadUsuario(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
    }

    private Material loadOwnedMaterial(String materialId, Usuario usuario) {
        UUID id = parseMaterialId(materialId);
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material nao encontrado"));

        if (!material.getDoador().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("Apenas o doador do material pode alterar este cadastro");
        }

        return material;
    }

    private UUID parseMaterialId(String materialId) {
        try {
            return UUID.fromString(materialId);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Identificador de material invalido", Map.of(
                    "id", "Informe um identificador de material valido"
            ));
        }
    }

    private void updatePreviewTracking(TemporaryUpload upload, GeminiResponseDTO response) {
        StatusIA statusIa = StatusIA.valueOf(response.getStatusIa());
        upload.setStatusIa(statusIa);
        upload.setStatus(switch (statusIa) {
            case SUCCESS -> UploadProcessingStatus.PREVIEW_SUCCESS;
            case LOW_CONFIDENCE -> UploadProcessingStatus.PREVIEW_LOW_CONFIDENCE;
            case FAILURE, NOT_ATTEMPTED -> UploadProcessingStatus.PREVIEW_FAILURE;
        });
        upload.setConfiancaIa(maxConfidence(response));
        temporaryUploadRepository.save(upload);
    }

    private BigDecimal maxConfidence(GeminiResponseDTO response) {
        return response.getBestPrediction().values().stream()
                .map(prediction -> prediction.getConfidence() == null ? null : BigDecimal.valueOf(prediction.getConfidence()))
                .filter(java.util.Objects::nonNull)
                .max(BigDecimal::compareTo)
                .map(value -> value.setScale(2, RoundingMode.HALF_UP))
                .orElse(null);
    }

    private ValidatedMaterialRequest validateCreateRequest(CreateMaterialRequestDTO request) {
        LinkedHashMap<String, String> errors = new LinkedHashMap<>();

        String uploadId = trimToNull(request.getUploadId());
        if (uploadId == null) {
            errors.put("upload_id", "Informe o upload_id retornado pelo preview");
        }

        ValidatedMaterialData data = validateMaterialData(
                request.getTitulo(),
                request.getAutor(),
                request.getEditora(),
                request.getDescricao(),
                request.getDisciplina(),
                request.getNivelEnsino(),
                request.getAno(),
                request.getSistemaEnsino(),
                request.getEstadoConservacao(),
                request.getDataPublicacao(),
                errors
        );

        if (!errors.isEmpty()) {
            throw new BadRequestException("Os dados do material sao invalidos", errors);
        }

        return new ValidatedMaterialRequest(uploadId, data);
    }

    private ValidatedMaterialData validateUpdateRequest(UpdateMaterialRequestDTO request) {
        LinkedHashMap<String, String> errors = new LinkedHashMap<>();
        ValidatedMaterialData data = validateMaterialData(
                request.getTitulo(),
                request.getAutor(),
                request.getEditora(),
                request.getDescricao(),
                request.getDisciplina(),
                request.getNivelEnsino(),
                request.getAno(),
                request.getSistemaEnsino(),
                request.getEstadoConservacao(),
                request.getDataPublicacao(),
                errors
        );

        if (!errors.isEmpty()) {
            throw new BadRequestException("Os dados do material sao invalidos", errors);
        }

        return data;
    }

    private ValidatedMaterialData validateMaterialData(String rawTitulo,
                                                       String rawAutor,
                                                       String rawEditora,
                                                       String rawDescricao,
                                                       String rawDisciplina,
                                                       String rawNivelEnsino,
                                                       Integer rawAno,
                                                       String rawSistemaEnsino,
                                                       String rawEstadoConservacao,
                                                       Integer rawDataPublicacao,
                                                       Map<String, String> errors) {
        String titulo = trimToNull(rawTitulo);
        if (titulo == null) {
            errors.put("titulo", "Informe o titulo do material");
        } else if (titulo.length() > 255) {
            errors.put("titulo", "O titulo deve ter no maximo 255 caracteres");
        }

        String autor = trimToNull(rawAutor);
        if (autor != null && autor.length() > 255) {
            errors.put("autor", "O autor deve ter no maximo 255 caracteres");
        }

        String editora = trimToNull(rawEditora);
        if (editora != null && editora.length() > 255) {
            errors.put("editora", "A editora deve ter no maximo 255 caracteres");
        }

        String descricao = trimToNull(rawDescricao);
        if (descricao == null) {
            errors.put("descricao", "Informe a descricao do material");
        } else if (descricao.length() < 10 || descricao.length() > 2000) {
            errors.put("descricao", "A descricao deve ter entre 10 e 2000 caracteres");
        }

        Disciplina disciplina = parseEnum(rawDisciplina, Disciplina.class, "disciplina", errors);
        NivelEnsino nivelEnsino = parseEnum(rawNivelEnsino, NivelEnsino.class, "nivel_ensino", errors);
        SistemaEnsino sistemaEnsino = parseEnum(rawSistemaEnsino, SistemaEnsino.class, "sistema_ensino", errors);
        EstadoConservacao estadoConservacao = parseEnum(rawEstadoConservacao, EstadoConservacao.class, "estado_conservacao", errors);

        Integer ano = rawAno;
        if (nivelEnsino != null) {
            if (nivelEnsino == NivelEnsino.SUPERIOR) {
                if (ano != null) {
                    errors.put("ano", "Materiais de nivel SUPERIOR nao usam ano escolar");
                }
            } else if (ano == null || ano < 1 || ano > 12) {
                errors.put("ano", "Informe um ano escolar entre 1 e 12");
            }
        }

        Integer dataPublicacao = rawDataPublicacao;
        if (dataPublicacao != null && (dataPublicacao < 1900 || dataPublicacao > 2100)) {
            errors.put("data_publicacao", "Informe um ano de publicacao entre 1900 e 2100");
        }

        return new ValidatedMaterialData(
                titulo,
                autor,
                editora,
                descricao,
                disciplina,
                nivelEnsino,
                ano,
                sistemaEnsino,
                estadoConservacao,
                dataPublicacao
        );
    }

    private void ensureStatusAllowsEditing(Material material) {
        if (material.getStatus() != StatusMaterial.DISPONIVEL) {
            throw new UnprocessableEntityException("Somente materiais disponiveis podem ser editados");
        }
    }

    private void ensureStatusAllowsCancellation(Material material) {
        if (material.getStatus() != StatusMaterial.DISPONIVEL) {
            throw new UnprocessableEntityException("Somente materiais disponiveis podem ser excluidos");
        }
    }

    private <E extends Enum<E>> E parseEnum(String rawValue,
                                            Class<E> enumType,
                                            String field,
                                            Map<String, String> errors) {
        String value = trimToNull(rawValue);
        if (value == null) {
            errors.put(field, "Informe um valor para " + field);
            return null;
        }

        try {
            return Enum.valueOf(enumType, normalizeEnum(value));
        } catch (IllegalArgumentException ex) {
            errors.put(field, "Valor invalido para " + field);
            return null;
        }
    }

    private String normalizeEnum(String value) {
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .toUpperCase(java.util.Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record ValidatedMaterialRequest(
            String uploadId,
            ValidatedMaterialData data
    ) {
    }

    private record ValidatedMaterialData(
            String titulo,
            String autor,
            String editora,
            String descricao,
            Disciplina disciplina,
            NivelEnsino nivelEnsino,
            Integer ano,
            SistemaEnsino sistemaEnsino,
            EstadoConservacao estadoConservacao,
            Integer dataPublicacao
    ) {
    }
}
