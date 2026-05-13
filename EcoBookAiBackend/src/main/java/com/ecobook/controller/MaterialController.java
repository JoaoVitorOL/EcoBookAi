package com.ecobook.controller;

import com.ecobook.annotation.RequireCompleteProfile;
import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import com.ecobook.dto.CreateMaterialRequestDTO;
import com.ecobook.dto.GeminiResponseDTO;
import com.ecobook.dto.MaterialDTO;
import com.ecobook.dto.PagedResponseDTO;
import com.ecobook.dto.SearchCriteriaDTO;
import com.ecobook.dto.UpdateMaterialRequestDTO;
import com.ecobook.exception.BadRequestException;
import com.ecobook.model.enums.Disciplina;
import com.ecobook.model.enums.NivelEnsino;
import com.ecobook.model.enums.SistemaEnsino;
import com.ecobook.service.MatchingService;
import com.ecobook.service.MaterialService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/v1/materiais")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    private final MatchingService matchingService;

    @GetMapping
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<PagedResponseDTO<MaterialDTO>>> searchMaterials(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String disciplina,
            @RequestParam(required = false, name = "nivel_ensino") String nivelEnsino,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false, name = "sistema_ensino") String sistemaEnsino,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String bairro,
            @RequestParam(required = false, name = "min_ano_publicacao") Integer minAnoPublicacao,
            @RequestParam(required = false, name = "max_ano_publicacao") Integer maxAnoPublicacao,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            HttpServletRequest servletRequest) {

        LinkedHashMap<String, String> fieldErrors = new LinkedHashMap<>();
        Disciplina parsedDisciplina = parseEnum(disciplina, Disciplina.class, "disciplina", fieldErrors);
        NivelEnsino parsedNivelEnsino = parseEnum(nivelEnsino, NivelEnsino.class, "nivel_ensino", fieldErrors);
        SistemaEnsino parsedSistemaEnsino = parseEnum(sistemaEnsino, SistemaEnsino.class, "sistema_ensino", fieldErrors);

        validatePagination(page, size, fieldErrors);
        validateYears(ano, minAnoPublicacao, maxAnoPublicacao, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new BadRequestException("Os filtros de busca sao invalidos", fieldErrors);
        }

        SearchCriteriaDTO criteria = SearchCriteriaDTO.builder()
                .query(query)
                .disciplina(parsedDisciplina)
                .nivelEnsino(parsedNivelEnsino)
                .ano(ano)
                .sistemaEnsino(parsedSistemaEnsino)
                .cidade(cidade)
                .bairro(bairro)
                .minAnoPublicacao(minAnoPublicacao)
                .maxAnoPublicacao(maxAnoPublicacao)
                .build();

        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Materiais encontrados com sucesso",
                matchingService.findMatching(criteria, PageRequest.of(page, size))
        );
    }

    @GetMapping("/me")
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<List<MaterialDTO>>> listCurrentUserMaterials(Authentication authentication,
                                                                                   HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Materiais do usuario carregados com sucesso",
                materialService.listCurrentUserMaterials(authentication.getName())
        );
    }

    @PostMapping("/preview")
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<GeminiResponseDTO>> previewMaterial(Authentication authentication,
                                                                          @RequestPart("file") MultipartFile file,
                                                                          HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Preview do material gerado com sucesso",
                materialService.previewMaterial(authentication.getName(), file)
        );
    }

    @PostMapping
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<MaterialDTO>> createMaterial(Authentication authentication,
                                                                   @RequestBody CreateMaterialRequestDTO request,
                                                                   HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.created(
                servletRequest,
                "Material publicado com sucesso",
                materialService.createMaterial(authentication.getName(), request)
        );
    }

    @PutMapping("/{id}")
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<MaterialDTO>> updateMaterial(@PathVariable String id,
                                                                   Authentication authentication,
                                                                   @RequestBody UpdateMaterialRequestDTO request,
                                                                   HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Material atualizado com sucesso",
                materialService.updateMaterial(authentication.getName(), id, request)
        );
    }

    @DeleteMapping("/{id}")
    @RequireCompleteProfile
    public ResponseEntity<Void> deleteMaterial(@PathVariable String id,
                                               Authentication authentication) {
        materialService.deleteMaterial(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    private void validatePagination(Integer page, Integer size, Map<String, String> fieldErrors) {
        if (page == null || page < 0) {
            fieldErrors.put("page", "Informe uma pagina maior ou igual a zero");
        }
        if (size == null || size < 1 || size > 100) {
            fieldErrors.put("size", "Informe um tamanho de pagina entre 1 e 100");
        }
    }

    private void validateYears(Integer ano,
                               Integer minAnoPublicacao,
                               Integer maxAnoPublicacao,
                               Map<String, String> fieldErrors) {
        if (ano != null && (ano < 1 || ano > 12)) {
            fieldErrors.put("ano", "Informe um ano escolar entre 1 e 12");
        }
        if (minAnoPublicacao != null && (minAnoPublicacao < 1900 || minAnoPublicacao > 2100)) {
            fieldErrors.put("min_ano_publicacao", "Informe um ano minimo entre 1900 e 2100");
        }
        if (maxAnoPublicacao != null && (maxAnoPublicacao < 1900 || maxAnoPublicacao > 2100)) {
            fieldErrors.put("max_ano_publicacao", "Informe um ano maximo entre 1900 e 2100");
        }
        if (minAnoPublicacao != null && maxAnoPublicacao != null && minAnoPublicacao > maxAnoPublicacao) {
            fieldErrors.put("ano_publicacao", "O ano minimo de publicacao nao pode ser maior que o maximo");
        }
    }

    private <E extends Enum<E>> E parseEnum(String rawValue,
                                            Class<E> enumType,
                                            String field,
                                            Map<String, String> fieldErrors) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String normalized = Normalizer.normalize(rawValue, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .toUpperCase(Locale.ROOT);

        try {
            return Enum.valueOf(enumType, normalized);
        } catch (IllegalArgumentException ex) {
            fieldErrors.put(field, "Valor invalido para " + field);
            return null;
        }
    }
}
