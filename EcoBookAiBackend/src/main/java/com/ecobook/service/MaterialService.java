package com.ecobook.service;

import com.ecobook.dto.CreateMaterialRequestDTO;
import com.ecobook.dto.GeminiResponseDTO;
import com.ecobook.dto.MaterialDTO;
import com.ecobook.dto.MaterialDonorDTO;
import com.ecobook.exception.BadRequestException;
import com.ecobook.exception.ConflictException;
import com.ecobook.exception.ResourceNotFoundException;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
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
                    .titulo(validated.titulo())
                    .autor(validated.autor())
                    .editora(validated.editora())
                    .descricao(validated.descricao())
                    .disciplina(validated.disciplina())
                    .nivelEnsino(validated.nivelEnsino())
                    .ano(validated.ano())
                    .sistemaEnsino(validated.sistemaEnsino())
                    .estadoConservacao(validated.estadoConservacao())
                    .status(StatusMaterial.DISPONIVEL)
                    .imagemUrl(promotedImage.publicUrl())
                    .uploadId(validated.uploadId())
                    .cidade(usuario.getCidade())
                    .bairro(usuario.getBairro())
                    .dataPublicacao(validated.dataPublicacao())
                    .statusIa(upload.getStatusIa())
                    .confiancaIa(upload.getConfiancaIa())
                    .build());

            upload.setMaterial(material);
            upload.setStatus(UploadProcessingStatus.MATERIAL_CREATED);
            upload.setExpiresAt(null);
            upload.setFilePath(promotedImage.absolutePath().toString());
            temporaryUploadRepository.save(upload);

            return toDto(material);
        } catch (RuntimeException ex) {
            log.error("Falha ao persistir material para upload {}", validated.uploadId(), ex);
            throw ex;
        }
    }

    MaterialDTO toDto(Material material) {
        return MaterialDTO.builder()
                .id(material.getId().toString())
                .titulo(material.getTitulo())
                .autor(material.getAutor())
                .editora(material.getEditora())
                .descricao(material.getDescricao())
                .disciplina(material.getDisciplina().name())
                .nivelEnsino(material.getNivelEnsino().name())
                .ano(material.getAno())
                .sistemaEnsino(material.getSistemaEnsino().name())
                .estadoConservacao(material.getEstadoConservacao().name())
                .status(material.getStatus().name())
                .imagemUrl(material.getImagemUrl())
                .uploadId(material.getUploadId())
                .doador(MaterialDonorDTO.builder()
                        .id(material.getDoador().getId().toString())
                        .nome(material.getDoador().getNome())
                        .whatsapp(material.getDoador().getWhatsapp())
                        .cidade(material.getDoador().getCidade())
                        .bairro(material.getDoador().getBairro())
                        .build())
                .cidade(material.getCidade())
                .bairro(material.getBairro())
                .dataPublicacao(material.getDataPublicacao())
                .statusIa(material.getStatusIa() != null ? material.getStatusIa().name() : null)
                .confiancaIa(material.getConfiancaIa() != null ? material.getConfiancaIa().doubleValue() : null)
                .criadoEm(material.getCriadoEm())
                .atualizadoEm(material.getAtualizadoEm())
                .build();
    }

    private Usuario loadUsuario(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
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

        String titulo = trimToNull(request.getTitulo());
        if (titulo == null) {
            errors.put("titulo", "Informe o titulo do material");
        } else if (titulo.length() > 255) {
            errors.put("titulo", "O titulo deve ter no maximo 255 caracteres");
        }

        String autor = trimToNull(request.getAutor());
        if (autor != null && autor.length() > 255) {
            errors.put("autor", "O autor deve ter no maximo 255 caracteres");
        }

        String editora = trimToNull(request.getEditora());
        if (editora != null && editora.length() > 255) {
            errors.put("editora", "A editora deve ter no maximo 255 caracteres");
        }

        String descricao = trimToNull(request.getDescricao());
        if (descricao == null) {
            errors.put("descricao", "Informe a descricao do material");
        } else if (descricao.length() < 10 || descricao.length() > 2000) {
            errors.put("descricao", "A descricao deve ter entre 10 e 2000 caracteres");
        }

        Disciplina disciplina = parseEnum(request.getDisciplina(), Disciplina.class, "disciplina", errors);
        NivelEnsino nivelEnsino = parseEnum(request.getNivelEnsino(), NivelEnsino.class, "nivel_ensino", errors);
        SistemaEnsino sistemaEnsino = parseEnum(request.getSistemaEnsino(), SistemaEnsino.class, "sistema_ensino", errors);
        EstadoConservacao estadoConservacao = parseEnum(request.getEstadoConservacao(), EstadoConservacao.class, "estado_conservacao", errors);

        Integer ano = request.getAno();
        if (nivelEnsino != null) {
            if (nivelEnsino == NivelEnsino.SUPERIOR) {
                if (ano != null) {
                    errors.put("ano", "Materiais de nivel SUPERIOR nao usam ano escolar");
                }
            } else if (ano == null || ano < 1 || ano > 12) {
                errors.put("ano", "Informe um ano escolar entre 1 e 12");
            }
        }

        Integer dataPublicacao = request.getDataPublicacao();
        if (dataPublicacao != null && (dataPublicacao < 1900 || dataPublicacao > 2100)) {
            errors.put("data_publicacao", "Informe um ano de publicacao entre 1900 e 2100");
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException("Os dados do material sao invalidos", errors);
        }

        return new ValidatedMaterialRequest(
                uploadId,
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
