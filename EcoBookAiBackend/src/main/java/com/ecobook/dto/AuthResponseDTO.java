package com.ecobook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AuthResponseDTO {
    private String id;
    private String email;
    private String nome;
    private String whatsapp;
    private String cpf;
    private String cidade;
    private String bairro;
    private String instituicao;
    private String fotoPerfilUrl;
    private Boolean perfilCompleto;
    private Boolean consentimentoIa;
    private Set<String> necessidadesAcademicas;
    private String role;
    private String token;
    private Long expiresIn;
}
