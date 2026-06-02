package com.ecobook.dto;

import com.ecobook.model.enums.Disciplina;
import com.ecobook.model.enums.NecessidadeAcademica;
import com.ecobook.model.enums.NivelEnsino;
import com.ecobook.model.enums.SistemaEnsino;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SearchCriteriaDTO {
    private String query;
    private Disciplina disciplina;
    private NivelEnsino nivelEnsino;
    private Integer ano;
    private SistemaEnsino sistemaEnsino;
    private String cidade;
    private String bairro;
    private Integer minAnoPublicacao;
    private Integer maxAnoPublicacao;
    private NecessidadeAcademica necessidadeAcademica;
}
