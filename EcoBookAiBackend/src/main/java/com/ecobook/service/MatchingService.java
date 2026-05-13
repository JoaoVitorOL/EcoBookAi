package com.ecobook.service;

import com.ecobook.dto.MaterialDTO;
import com.ecobook.dto.PagedResponseDTO;
import com.ecobook.dto.SearchCriteriaDTO;
import com.ecobook.model.Material;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Matching algorithm service for material discovery
 */
@Service
@RequiredArgsConstructor
public class MatchingService {

    private final MaterialRepository materialRepository;
    private final GeoNormalizationService geoNormalizationService;
    private final MaterialMapper materialMapper;

    @Transactional(readOnly = true)
    public PagedResponseDTO<MaterialDTO> findMatching(SearchCriteriaDTO criteria, Pageable pageable) {
        SearchCriteriaDTO normalizedCriteria = normalize(criteria);

        List<MaterialDTO> rankedResults = materialRepository.findByStatus(StatusMaterial.DISPONIVEL).stream()
                .filter(material -> matchesQuery(material, normalizedCriteria.getQuery()))
                .filter(material -> matchesDisciplina(material, normalizedCriteria))
                .filter(material -> matchesNivelEnsino(material, normalizedCriteria))
                .filter(material -> matchesYear(material, normalizedCriteria))
                .filter(material -> matchesSystem(material, normalizedCriteria))
                .filter(material -> matchesPublicationYear(material, normalizedCriteria))
                .sorted(createComparator(normalizedCriteria))
                .map(materialMapper::toDto)
                .toList();

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int start = Math.min((int) pageable.getOffset(), rankedResults.size());
        int end = Math.min(start + size, rankedResults.size());

        return PagedResponseDTO.of(rankedResults.subList(start, end), page, size, rankedResults.size());
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

    Comparator<Material> createComparator(SearchCriteriaDTO criteria) {
        return Comparator
                .comparing((Material material) -> !isSameNeighborhood(material, criteria))
                .thenComparing(material -> !isSameCity(material, criteria))
                .thenComparing(Material::getDataPublicacao, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(material -> material.getId().toString());
    }

    private boolean matchesQuery(Material material, String query) {
        if (!StringUtils.hasText(query)) {
            return true;
        }

        String normalizedQuery = normalizeText(query);
        return Stream.of(
                        material.getTitulo(),
                        material.getDescricao(),
                        material.getAutor(),
                        material.getEditora(),
                        material.getCidade(),
                        material.getBairro(),
                        material.getDoador() == null ? null : material.getDoador().getNome()
                )
                .filter(StringUtils::hasText)
                .map(this::normalizeText)
                .anyMatch(value -> value.contains(normalizedQuery));
    }

    private boolean matchesDisciplina(Material material, SearchCriteriaDTO criteria) {
        return criteria.getDisciplina() == null || material.getDisciplina() == criteria.getDisciplina();
    }

    private boolean matchesNivelEnsino(Material material, SearchCriteriaDTO criteria) {
        return criteria.getNivelEnsino() == null || material.getNivelEnsino() == criteria.getNivelEnsino();
    }

    private boolean matchesYear(Material material, SearchCriteriaDTO criteria) {
        if (criteria.getAno() == null) {
            return true;
        }
        if (material.getNivelEnsino() == com.ecobook.model.enums.NivelEnsino.SUPERIOR) {
            return true;
        }
        if (material.getAno() == null) {
            return false;
        }

        int targetYear = criteria.getAno();
        return material.getAno() >= targetYear - 1 && material.getAno() <= targetYear + 1;
    }

    private boolean matchesSystem(Material material, SearchCriteriaDTO criteria) {
        if (criteria.getSistemaEnsino() == null) {
            return true;
        }
        if (criteria.getSistemaEnsino() == com.ecobook.model.enums.SistemaEnsino.OUTRO) {
            return material.getSistemaEnsino() == com.ecobook.model.enums.SistemaEnsino.OUTRO;
        }
        return material.getSistemaEnsino() == criteria.getSistemaEnsino()
                || material.getSistemaEnsino() == com.ecobook.model.enums.SistemaEnsino.OUTRO;
    }

    private boolean matchesPublicationYear(Material material, SearchCriteriaDTO criteria) {
        Integer publicationYear = material.getDataPublicacao();
        if (criteria.getMinAnoPublicacao() != null) {
            if (publicationYear == null || publicationYear < criteria.getMinAnoPublicacao()) {
                return false;
            }
        }
        if (criteria.getMaxAnoPublicacao() != null) {
            if (publicationYear == null || publicationYear > criteria.getMaxAnoPublicacao()) {
                return false;
            }
        }
        return true;
    }

    private boolean isSameNeighborhood(Material material, SearchCriteriaDTO criteria) {
        return StringUtils.hasText(criteria.getBairro())
                && criteria.getBairro().equals(material.getBairro());
    }

    private boolean isSameCity(Material material, SearchCriteriaDTO criteria) {
        return StringUtils.hasText(criteria.getCidade())
                && criteria.getCidade().equals(material.getCidade());
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
