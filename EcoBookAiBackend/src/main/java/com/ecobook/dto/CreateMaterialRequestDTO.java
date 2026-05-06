package com.ecobook.dto;

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
public class CreateMaterialRequestDTO {
    private String uploadId;
    private String titulo;
    private String descricao;
    private String disciplina;
    private String nivelEnsino;
    private Integer ano;
    private String sistemaEnsino;
    private String estadoConservacao;
    private Integer dataPublicacao;
}
