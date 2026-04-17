package com.ecobook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private String id;
    private String email;
    private String nome;
    private Boolean perfilCompleto;
    private String role;
    private String token;
    private Long expiresIn;
}
