package com.ecobook.service;

import com.ecobook.config.CacheNames;
import com.ecobook.dto.ReferenceDataCatalogDTO;
import com.ecobook.dto.ReferenceOptionDTO;
import com.ecobook.model.enums.Disciplina;
import com.ecobook.model.enums.EstadoConservacao;
import com.ecobook.model.enums.NecessidadeAcademica;
import com.ecobook.model.enums.NivelEnsino;
import com.ecobook.model.enums.SistemaEnsino;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Service
public class ReferenceDataService {

    /**
     * Returns the cached catalog of material reference options.
     * @return requested value
     */
    @Cacheable(value = CacheNames.REFERENCE_DATA_MATERIAL_OPTIONS, sync = true)
    public ReferenceDataCatalogDTO getMaterialOptions() {
        return ReferenceDataCatalogDTO.builder()
                .disciplinas(toOptions(Disciplina.values(), Disciplina::getLabel))
                .niveisEnsino(toOptions(NivelEnsino.values(), NivelEnsino::getLabel))
                .sistemasEnsino(toOptions(SistemaEnsino.values(), SistemaEnsino::getLabel))
                .estadosConservacao(toOptions(EstadoConservacao.values(), EstadoConservacao::getLabel))
                .necessidadesAcademicas(toOptions(NecessidadeAcademica.values(), NecessidadeAcademica::getDescription))
                .build();
    }

    private <E extends Enum<E>> List<ReferenceOptionDTO> toOptions(E[] values, Function<E, String> labelMapper) {
        return Arrays.stream(values)
                .map(value -> ReferenceOptionDTO.builder()
                        .value(value.name())
                        .label(labelMapper.apply(value))
                        .build())
                .toList();
    }
}
