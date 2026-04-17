package com.ecobook.dto;

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
public class UsuarioDTO {
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
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
