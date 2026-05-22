package com.ecobook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DeleteAccountRequestDTO {

    @NotBlank(message = "Informe sua senha para confirmar a exclusão da conta")
    @Size(max = 72, message = "A senha informada é inválida")
    @ToString.Exclude
    private String password;

    @Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres")
    private String reason;
}
