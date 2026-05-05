package com.ecobook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitacaoDTO {
    private String id;
    private String materialId;
    private String estudanteId;
    private String status;
    private Map<String, String> contatoDoador;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private LocalDateTime aprovadoEm;
    private LocalDateTime expiresAt;
}
