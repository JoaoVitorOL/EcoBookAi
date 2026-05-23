package com.ecobook.service;

import com.ecobook.dto.MaterialDTO;
import com.ecobook.dto.PagedResponseDTO;
import com.ecobook.dto.SearchCriteriaDTO;
import com.ecobook.exception.BadRequestException;
import com.ecobook.model.Material;
import com.ecobook.model.enums.Disciplina;
import com.ecobook.model.enums.NivelEnsino;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.repository.MaterialRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Matching algorithm service for material discovery.
 */
@Service
@RequiredArgsConstructor
public class MatchingService {

    private static final String ACCENTED_CHARS =
            "\u00E1\u00E0\u00E2\u00E3\u00E4\u00E9\u00E8\u00EA\u00EB\u00ED\u00EC\u00EE\u00EF"
                    + "\u00F3\u00F2\u00F4\u00F5\u00F6\u00FA\u00F9\u00FB\u00FC\u00E7\u00F1\u00FD\u00FF";
    private static final String ASCII_CHARS = "aaaaaeeeeiiiiooooouuuucnyy";

    private final MaterialRepository materialRepository;
    private final GeoNormalizationService geoNormalizationService;
    private final MaterialMapper materialMapper;

    @Transactional(readOnly = true)
    public PagedResponseDTO<MaterialDTO> findMatching(SearchCriteriaDTO criteria, Pageable pageable) {
        return findMatching(criteria, pageable, null);
    }

    @Transactional(readOnly = true)
    public PagedResponseDTO<MaterialDTO> findMatching(SearchCriteriaDTO criteria, Pageable pageable, String afterId) {
        SearchCriteriaDTO normalizedCriteria = normalize(criteria);
        Specification<Material> baseSpecification = buildSpecification(normalizedCriteria);

        if (!StringUtils.hasText(afterId)) {
            Page<Material> pageResult = materialRepository.findAll(baseSpecification, pageable);
            List<Material> pageMaterials = pageResult.getContent();
            List<MaterialDTO> results = pageMaterials.stream()
                    .map(materialMapper::toDto)
                    .toList();
            PagedResponseDTO<MaterialDTO> response = PagedResponseDTO.of(
                    results,
                    pageResult.getNumber(),
                    pageResult.getSize(),
                    pageResult.getTotalElements()
            );
            if (pageResult.hasNext() && !pageMaterials.isEmpty()) {
                response.setNextAfterId(pageMaterials.get(pageMaterials.size() - 1).getId().toString());
            }
            return response;
        }

        Material cursorMaterial = loadCursorMaterial(baseSpecification, afterId);
        Specification<Material> cursorSpecification = baseSpecification.and(
                buildAfterCursorSpecification(normalizedCriteria, cursorMaterial)
        );

        int requestedSize = pageable.getPageSize();
        Page<Material> keysetPage = materialRepository.findAll(
                cursorSpecification,
                PageRequest.of(0, requestedSize + 1)
        );

        List<Material> window = keysetPage.getContent();
        boolean hasNext = window.size() > requestedSize;
        List<Material> visibleMaterials = hasNext ? window.subList(0, requestedSize) : window;
        List<MaterialDTO> results = visibleMaterials.stream()
                .map(materialMapper::toDto)
                .toList();
        String nextAfterId = hasNext && !visibleMaterials.isEmpty()
                ? visibleMaterials.get(visibleMaterials.size() - 1).getId().toString()
                : null;

        return PagedResponseDTO.keyset(
                results,
                pageable.getPageNumber(),
                requestedSize,
                materialRepository.count(baseSpecification),
                hasNext,
                afterId,
                nextAfterId
        );
    }

    SearchCriteriaDTO normalize(SearchCriteriaDTO criteria) {
        if (criteria == null) {
            return SearchCriteriaDTO.builder().build();
        }

        String city = trimToNull(criteria.getCidade());
        String neighborhood = trimToNull(criteria.getBairro());

        return SearchCriteriaDTO.builder()
                .query(trimToNull(criteria.getQuery()))
                .disciplina(criteria.getDisciplina())
                .nivelEnsino(criteria.getNivelEnsino())
                .ano(criteria.getAno())
                .sistemaEnsino(criteria.getSistemaEnsino())
                .cidade(city == null ? null : geoNormalizationService.normalize(city))
                .bairro(neighborhood == null ? null : geoNormalizationService.normalize(neighborhood))
                .minAnoPublicacao(criteria.getMinAnoPublicacao())
                .maxAnoPublicacao(criteria.getMaxAnoPublicacao())
                .build();
    }

    private Specification<Material> buildSpecification(SearchCriteriaDTO criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Material, ?> donorJoin = root.join("doador", JoinType.LEFT);

            predicates.add(criteriaBuilder.equal(root.get("status"), StatusMaterial.DISPONIVEL));
            predicates.add(buildQueryPredicate(root, donorJoin, criteriaBuilder, criteria));

            if (criteria.getDisciplina() != null) {
                if (criteria.getDisciplina() == Disciplina.TODAS) {
                    predicates.add(criteriaBuilder.equal(root.get("disciplina"), Disciplina.TODAS));
                } else {
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.equal(root.get("disciplina"), criteria.getDisciplina()),
                            criteriaBuilder.equal(root.get("disciplina"), Disciplina.TODAS)
                    ));
                }
            }

            if (criteria.getNivelEnsino() != null) {
                predicates.add(criteriaBuilder.equal(root.get("nivelEnsino"), criteria.getNivelEnsino()));
            }

            if (criteria.getAno() != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("nivelEnsino"), NivelEnsino.SUPERIOR),
                        criteriaBuilder.and(
                                criteriaBuilder.isNotNull(root.get("ano")),
                                criteriaBuilder.between(root.get("ano"), criteria.getAno() - 1, criteria.getAno() + 1)
                        )
                ));
            }

            if (criteria.getSistemaEnsino() != null) {
                predicates.add(criteriaBuilder.equal(root.get("sistemaEnsino"), criteria.getSistemaEnsino()));
            }

            if (criteria.getMinAnoPublicacao() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("dataPublicacao"),
                        criteria.getMinAnoPublicacao()
                ));
            }

            if (criteria.getMaxAnoPublicacao() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("dataPublicacao"),
                        criteria.getMaxAnoPublicacao()
                ));
            }

            applyOrdering(root, query, criteriaBuilder, criteria);
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<Material> buildAfterCursorSpecification(SearchCriteriaDTO criteria, Material cursorMaterial) {
        return (root, query, criteriaBuilder) -> {
            Expression<Integer> neighborhoodRank = neighborhoodRankExpression(root, criteriaBuilder, criteria);
            Expression<Integer> cityRank = cityRankExpression(root, criteriaBuilder, criteria);
            Expression<Integer> publicationRank = publicationRankExpression(root, criteriaBuilder);

            int cursorNeighborhoodRank = neighborhoodRank(cursorMaterial, criteria);
            int cursorCityRank = cityRank(cursorMaterial, criteria);
            int cursorPublicationRank = publicationRank(cursorMaterial);

            Predicate afterNeighborhood = criteriaBuilder.greaterThan(neighborhoodRank, cursorNeighborhoodRank);
            Predicate afterCity = criteriaBuilder.and(
                    criteriaBuilder.equal(neighborhoodRank, cursorNeighborhoodRank),
                    criteriaBuilder.greaterThan(cityRank, cursorCityRank)
            );
            Predicate afterPublication = criteriaBuilder.and(
                    criteriaBuilder.equal(neighborhoodRank, cursorNeighborhoodRank),
                    criteriaBuilder.equal(cityRank, cursorCityRank),
                    criteriaBuilder.lessThan(publicationRank, cursorPublicationRank)
            );
            Predicate afterId = criteriaBuilder.and(
                    criteriaBuilder.equal(neighborhoodRank, cursorNeighborhoodRank),
                    criteriaBuilder.equal(cityRank, cursorCityRank),
                    criteriaBuilder.equal(publicationRank, cursorPublicationRank),
                    criteriaBuilder.greaterThan(root.get("id"), cursorMaterial.getId())
            );

            applyOrdering(root, query, criteriaBuilder, criteria);
            return criteriaBuilder.or(afterNeighborhood, afterCity, afterPublication, afterId);
        };
    }

    private Material loadCursorMaterial(Specification<Material> baseSpecification, String afterId) {
        UUID cursorId;
        try {
            cursorId = UUID.fromString(afterId);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "O cursor after_id e invalido",
                    Map.of("after_id", "Informe um UUID valido para after_id")
            );
        }

        return materialRepository.findOne(
                        baseSpecification.and((root, query, criteriaBuilder) ->
                                criteriaBuilder.equal(root.get("id"), cursorId))
                )
                .orElseThrow(() -> new BadRequestException(
                        "O cursor after_id nao pertence ao resultado atual",
                        Map.of("after_id", "Use um after_id retornado pela pagina anterior com os mesmos filtros")
                ));
    }

    private Predicate buildQueryPredicate(Root<Material> root,
                                          Join<Material, ?> donorJoin,
                                          CriteriaBuilder criteriaBuilder,
                                          SearchCriteriaDTO criteria) {
        if (!StringUtils.hasText(criteria.getQuery())) {
            return criteriaBuilder.conjunction();
        }

        String likeQuery = "%" + normalizeText(criteria.getQuery()) + "%";
        return criteriaBuilder.or(
                criteriaBuilder.like(normalizedTextExpression(root.get("titulo"), criteriaBuilder), likeQuery),
                criteriaBuilder.like(normalizedTextExpression(root.get("descricao"), criteriaBuilder), likeQuery),
                criteriaBuilder.like(normalizedTextExpression(root.get("autor"), criteriaBuilder), likeQuery),
                criteriaBuilder.like(normalizedTextExpression(root.get("editora"), criteriaBuilder), likeQuery),
                criteriaBuilder.like(normalizedTextExpression(root.get("cidade"), criteriaBuilder), likeQuery),
                criteriaBuilder.like(normalizedTextExpression(root.get("bairro"), criteriaBuilder), likeQuery),
                criteriaBuilder.like(normalizedTextExpression(donorJoin.get("nome"), criteriaBuilder), likeQuery)
        );
    }

    private void applyOrdering(Root<Material> root,
                               jakarta.persistence.criteria.CriteriaQuery<?> query,
                               CriteriaBuilder criteriaBuilder,
                               SearchCriteriaDTO criteria) {
        if (Long.class.equals(query.getResultType()) || long.class.equals(query.getResultType())) {
            return;
        }

        List<Order> ordering = List.of(
                criteriaBuilder.asc(neighborhoodRankExpression(root, criteriaBuilder, criteria)),
                criteriaBuilder.asc(cityRankExpression(root, criteriaBuilder, criteria)),
                criteriaBuilder.desc(publicationRankExpression(root, criteriaBuilder)),
                criteriaBuilder.asc(root.get("id"))
        );
        query.orderBy(ordering);
    }

    private Expression<Integer> neighborhoodRankExpression(Root<Material> root,
                                                           CriteriaBuilder criteriaBuilder,
                                                           SearchCriteriaDTO criteria) {
        if (!StringUtils.hasText(criteria.getBairro())) {
            return criteriaBuilder.<Integer>selectCase()
                    .when(criteriaBuilder.isNotNull(root.get("id")), 1)
                    .otherwise(1);
        }

        return criteriaBuilder.<Integer>selectCase()
                .when(criteriaBuilder.equal(root.get("bairro"), criteria.getBairro()), 0)
                .otherwise(1);
    }

    private Expression<Integer> cityRankExpression(Root<Material> root,
                                                   CriteriaBuilder criteriaBuilder,
                                                   SearchCriteriaDTO criteria) {
        if (!StringUtils.hasText(criteria.getCidade())) {
            return criteriaBuilder.<Integer>selectCase()
                    .when(criteriaBuilder.isNotNull(root.get("id")), 1)
                    .otherwise(1);
        }

        return criteriaBuilder.<Integer>selectCase()
                .when(criteriaBuilder.equal(root.get("cidade"), criteria.getCidade()), 0)
                .otherwise(1);
    }

    private Expression<Integer> publicationRankExpression(Root<Material> root, CriteriaBuilder criteriaBuilder) {
        return criteriaBuilder.coalesce(root.get("dataPublicacao"), Integer.MIN_VALUE);
    }

    private Expression<String> normalizedTextExpression(Expression<String> source, CriteriaBuilder criteriaBuilder) {
        Expression<String> safeSource = criteriaBuilder.coalesce(source, "");
        Expression<String> lowered = criteriaBuilder.lower(safeSource);
        return criteriaBuilder.function(
                "translate",
                String.class,
                lowered,
                criteriaBuilder.literal(ACCENTED_CHARS),
                criteriaBuilder.literal(ASCII_CHARS)
        );
    }

    private int neighborhoodRank(Material material, SearchCriteriaDTO criteria) {
        return StringUtils.hasText(criteria.getBairro()) && criteria.getBairro().equals(material.getBairro()) ? 0 : 1;
    }

    private int cityRank(Material material, SearchCriteriaDTO criteria) {
        return StringUtils.hasText(criteria.getCidade()) && criteria.getCidade().equals(material.getCidade()) ? 0 : 1;
    }

    private int publicationRank(Material material) {
        return material.getDataPublicacao() == null ? Integer.MIN_VALUE : material.getDataPublicacao();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeText(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase(Locale.ROOT);
    }
}
