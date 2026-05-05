package com.ecobook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Email;
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
public class LoginRequestDTO {

    @NotBlank(message = "Informe seu email")
    @Email(message = "Informe um email valido")
    @Size(max = 255, message = "O email deve ter no maximo 255 caracteres")
    private String email;

    @NotBlank(message = "Informe sua senha")
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
    @ToString.Exclude
    private String password;
}
