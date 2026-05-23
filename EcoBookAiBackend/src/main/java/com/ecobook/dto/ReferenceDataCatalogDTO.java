package com.ecobook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ReferenceDataCatalogDTO {
    private List<ReferenceOptionDTO> disciplinas;
    private List<ReferenceOptionDTO> niveisEnsino;
    private List<ReferenceOptionDTO> sistemasEnsino;
    private List<ReferenceOptionDTO> estadosConservacao;
    private List<ReferenceOptionDTO> necessidadesAcademicas;
}
