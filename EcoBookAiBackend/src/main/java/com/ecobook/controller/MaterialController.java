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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/materiais")
@RequiredArgsConstructor
@Tag(name = "Materiais", description = "Discovery, preview IA e gestao de materiais doados")
@SecurityRequirement(name = "bearer-jwt")
public class MaterialController {

    private final MaterialService materialService;
    private final MatchingService matchingService;

    /**
     * Handles the search materials request.
     *
     * @param query the query value
     * @param disciplina the disciplina value
     * @param nivelEnsino the nivelEnsino value
     * @param ano the ano value
     * @param sistemaEnsino the sistemaEnsino value
     * @param cidade the cidade value
     * @param bairro the bairro value
     * @param minAnoPublicacao the minAnoPublicacao value
     * @param maxAnoPublicacao the maxAnoPublicacao value
     * @param afterId the cursor identifier for keyset pagination
     * @param page the requested page index
     * @param size the requested page size
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @GetMapping
    @RequireCompleteProfile
    @Operation(summary = "Buscar materiais", description = "Executa a discovery com filtros academicos, geograficos e paginacao offset/cursor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Materiais encontrados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Filtros invalidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto")
    })
    public ResponseEntity<ApiEnvelope<PagedResponseDTO<MaterialDTO>>> searchMaterials(
            @RequestParam(required = false) @Parameter(description = "Texto livre usado na busca por titulo, descricao, autor, editora e localidade") String query,
            @RequestParam(required = false) @Parameter(description = "Filtro por disciplina") String disciplina,
            @RequestParam(required = false, name = "nivel_ensino") @Parameter(description = "Filtro por nivel de ensino") String nivelEnsino,
            @RequestParam(required = false) @Parameter(description = "Ano escolar quando aplicavel ao nivel de ensino") Integer ano,
            @RequestParam(required = false, name = "sistema_ensino") @Parameter(description = "Filtro por sistema de ensino") String sistemaEnsino,
            @RequestParam(required = false) @Parameter(description = "Cidade do material") String cidade,
            @RequestParam(required = false) @Parameter(description = "Bairro do material") String bairro,
            @RequestParam(required = false, name = "min_ano_publicacao") @Parameter(description = "Ano minimo de publicacao") Integer minAnoPublicacao,
            @RequestParam(required = false, name = "max_ano_publicacao") @Parameter(description = "Ano maximo de publicacao") Integer maxAnoPublicacao,
            @RequestParam(required = false, name = "after_id") @Parameter(description = "Cursor UUID para continuar a pagina seguinte no modo keyset") String afterId,
            @RequestParam(defaultValue = "0") @Parameter(description = "Pagina offset inicial") Integer page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Quantidade de itens por pagina, entre 1 e 100") Integer size,
            HttpServletRequest servletRequest) {

        LinkedHashMap<String, String> fieldErrors = new LinkedHashMap<>();
        Disciplina parsedDisciplina = parseEnum(disciplina, Disciplina.class, "disciplina", fieldErrors);
        NivelEnsino parsedNivelEnsino = parseEnum(nivelEnsino, NivelEnsino.class, "nivel_ensino", fieldErrors);
        SistemaEnsino parsedSistemaEnsino = parseEnum(sistemaEnsino, SistemaEnsino.class, "sistema_ensino", fieldErrors);

        validatePagination(page, size, fieldErrors);
        validateAfterId(afterId, fieldErrors);
        validateYears(parsedNivelEnsino, ano, minAnoPublicacao, maxAnoPublicacao, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new BadRequestException("Os filtros de busca sao invalidos", fieldErrors);
        }

        Integer effectiveAno = parsedNivelEnsino == NivelEnsino.SUPERIOR ? null : ano;

        SearchCriteriaDTO criteria = SearchCriteriaDTO.builder()
                .query(query)
                .disciplina(parsedDisciplina)
                .nivelEnsino(parsedNivelEnsino)
                .ano(effectiveAno)
                .sistemaEnsino(parsedSistemaEnsino)
                .cidade(cidade)
                .bairro(bairro)
                .minAnoPublicacao(minAnoPublicacao)
                .maxAnoPublicacao(maxAnoPublicacao)
                .build();

        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Materiais encontrados com sucesso",
                matchingService.findMatching(criteria, PageRequest.of(page, size), afterId)
        );
    }

    /**
     * Handles the list current user materials request.
     *
     * @param authentication the authenticated principal context
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @GetMapping("/me")
    @RequireCompleteProfile
    @Operation(summary = "Listar materiais do doador", description = "Retorna todos os materiais publicados pelo usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Materiais carregados com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto")
    })
    public ResponseEntity<ApiEnvelope<List<MaterialDTO>>> listCurrentUserMaterials(Authentication authentication,
                                                                                   HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Materiais do usuario carregados com sucesso",
                materialService.listCurrentUserMaterials(authentication.getName())
        );
    }

    /**
     * Handles the preview material request.
     *
     * @param authentication the authenticated principal context
     * @param legacyFile the legacyFile value
     * @param frontFile the frontFile value
     * @param backFile the backFile value
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PostMapping("/preview")
    @RequireCompleteProfile
    @Operation(
            summary = "Gerar preview IA",
            description = "Recebe a imagem frontal obrigatoria e a traseira opcional, cria o upload temporario e retorna o preview assistido por IA."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preview gerado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Arquivo invalido ou acima do limite"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto")
    })
    public ResponseEntity<ApiEnvelope<GeminiResponseDTO>> previewMaterial(Authentication authentication,
                                                                          @RequestPart(value = "file", required = false) @Parameter(description = "Alias legado da imagem frontal") MultipartFile legacyFile,
                                                                          @RequestPart(value = "file_front", required = false) @Parameter(description = "Imagem frontal do material") MultipartFile frontFile,
                                                                          @RequestPart(value = "file_back", required = false) @Parameter(description = "Imagem traseira opcional do material") MultipartFile backFile,
                                                                          HttpServletRequest servletRequest) {
        MultipartFile effectiveFrontFile = frontFile != null && !frontFile.isEmpty() ? frontFile : legacyFile;
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Preview do material gerado com sucesso",
                materialService.previewMaterial(authentication.getName(), effectiveFrontFile, backFile)
        );
    }

    /**
     * Handles the create material request.
     *
     * @param authentication the authenticated principal context
     * @param request the request payload
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PostMapping
    @RequireCompleteProfile
    @Operation(
            summary = "Publicar material",
            description = "Converte um upload temporario validado em material publicado e promovido no catalogo.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Metadados finais do material mais o upload_id retornado pelo preview"
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Material publicado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Payload invalido"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto"),
            @ApiResponse(responseCode = "404", description = "Upload temporario nao encontrado ou expirado"),
            @ApiResponse(responseCode = "409", description = "upload_id ja utilizado")
    })
    public ResponseEntity<ApiEnvelope<MaterialDTO>> createMaterial(Authentication authentication,
                                                                   @RequestBody CreateMaterialRequestDTO request,
                                                                   HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.created(
                servletRequest,
                "Material publicado com sucesso",
                materialService.createMaterial(authentication.getName(), request)
        );
    }

    /**
     * Handles the update material request.
     *
     * @param id the resource identifier
     * @param authentication the authenticated principal context
     * @param request the request payload
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PutMapping("/{id}")
    @RequireCompleteProfile
    @Operation(
            summary = "Atualizar material",
            description = "Atualiza os metadados de um material pertencente ao usuario autenticado.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Campos editaveis do material"
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Material atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Payload invalido"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto"),
            @ApiResponse(responseCode = "404", description = "Material nao encontrado")
    })
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

    /**
     * Handles the delete material request.
     *
     * @param id the resource identifier
     * @param authentication the authenticated principal context
     * @return the HTTP response for the request
     */
    @DeleteMapping("/{id}")
    @RequireCompleteProfile
    @Operation(summary = "Excluir material", description = "Exclui um material do usuario autenticado e limpa os artefatos associados quando existirem.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Material excluido com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Perfil incompleto"),
            @ApiResponse(responseCode = "404", description = "Material nao encontrado")
    })
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

    private void validateAfterId(String afterId, Map<String, String> fieldErrors) {
        if (afterId == null || afterId.isBlank()) {
            return;
        }

        try {
            UUID.fromString(afterId);
        } catch (IllegalArgumentException ex) {
            fieldErrors.put("after_id", "Informe um UUID valido para after_id");
        }
    }

    private void validateYears(NivelEnsino nivelEnsino,
                               Integer ano,
                               Integer minAnoPublicacao,
                               Integer maxAnoPublicacao,
                               Map<String, String> fieldErrors) {
        if (nivelEnsino != NivelEnsino.SUPERIOR && ano != null) {
            int maxAno = nivelEnsino == NivelEnsino.MEDIO ? 3 : 9;
            if (ano < 1 || ano > maxAno) {
                fieldErrors.put("ano", "Informe um ano escolar valido para o nivel selecionado");
            }
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
