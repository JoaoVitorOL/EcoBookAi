package com.ecobook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FcmTokenRequestDTO {

    @NotBlank(message = "Informe o token FCM do dispositivo")
    @Size(max = 512, message = "O token FCM excede o tamanho maximo permitido")
    private String token;
}
