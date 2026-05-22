package com.ecobook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdminUserSummaryDTO {
    private String id;
    private String email;
    private String nome;
    private String whatsapp;
    private String cidade;
    private String bairro;
    private String instituicao;
    private Boolean perfilCompleto;
    private Boolean consentimentoIa;
    private String role;
    private Set<String> necessidadesAcademicas;
    private Long materialsCount;
    private Long donatedMaterialsCount;
    private Long requestsCount;
    private Long completedRequestsCount;
    private Long openReportsCount;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
