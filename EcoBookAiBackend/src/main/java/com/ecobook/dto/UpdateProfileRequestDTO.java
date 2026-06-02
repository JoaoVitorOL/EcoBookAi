package com.ecobook.dto;

import com.ecobook.model.enums.NecessidadeAcademica;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
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
public class UpdateProfileRequestDTO {
    @Email(message = "Informe um email valido")
    @Size(max = 255, message = "O email deve ter no maximo 255 caracteres")
    private String email;
    private String nome;
    private String whatsapp;
    private String cpf;
    private String cidade;
    private String bairro;
    private String instituicao;
    private Boolean consentimentoIa;
    private Set<NecessidadeAcademica> necessidadesAcademicas;
}
