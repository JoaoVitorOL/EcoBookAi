package com.ecobook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MaterialDTO {
    private String id;
    private String titulo;
    private String autor;
    private String editora;
    private String descricao;
    private String disciplina;
    private String nivelEnsino;
    private Integer ano;
    private String sistemaEnsino;
    private String estadoConservacao;
    private String status;
    private String imagemUrl;
    private String uploadId;
    private MaterialDonorDTO doador;
    private String cidade;
    private String bairro;
    private Integer dataPublicacao;
    private String statusIa;
    private Double confiancaIa;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
