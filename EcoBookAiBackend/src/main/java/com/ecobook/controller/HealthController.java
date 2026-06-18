package com.ecobook.controller;

import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/health")
@Tag(name = "Saúde", description = "Verificação pública de disponibilidade do backend")
public class HealthController {

    /**
     * Returns the backend health payload for the current request.
     * @param servletRequest current HTTP servlet request
     * @return result of the operation
     */
    @GetMapping
    @Operation(summary = "Consultar saúde da API", description = "Retorna o status público mínimo do backend EcoBook AI.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Backend online")
    })
    public ResponseEntity<ApiEnvelope<Map<String, Object>>> health(HttpServletRequest servletRequest) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("application", "EcoBook IA Backend");
        response.put("version", "1.0.0-SNAPSHOT");
        return ApiEnvelopeResponses.ok(servletRequest, "Backend online", response);
    }
}
