package com.ecobook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialDTO {
    private String id;
    private String doadorId;
    private String titulo;
    private String descricao;
    private String disciplina;
    private String nivelEnsino;
    private Integer ano;
    private String sistemaEnsino;
    private String estadoConservacao;
    private String status;
    private String imagemUrl;
    private String uploadId;
    private String cidade;
    private String bairro;
    private Integer dataPublicacao;
    private String statusIa;
    private Double confiancaIa;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
