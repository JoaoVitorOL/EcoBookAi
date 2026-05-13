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
public class SolicitacaoMaterialDTO {
    private String id;
    private String titulo;
    private String descricao;
    private String imagemUrl;
    private String disciplina;
    private String nivelEnsino;
    private Integer ano;
    private String status;
    private String cidade;
    private String bairro;
    private String doadorNome;
}
