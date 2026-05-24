package com.ecobook.controller;

import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import com.ecobook.dto.ReferenceDataCatalogDTO;
import com.ecobook.service.ReferenceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/reference-data")
@RequiredArgsConstructor
@Tag(name = "Reference Data", description = "Catalogos publicos e cacheados para onboarding e materiais")
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    /**
     * Returns the cached reference catalog used by onboarding, discovery and publishing flows.
     * @param servletRequest current HTTP servlet request
     * @return requested value
     */
    @GetMapping("/material-options")
    @Operation(
            summary = "Listar catalogos de material",
            description = "Retorna os enums e labels usados pelo app Android para filtros, onboarding e publicacao de materiais."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catalogos carregados com sucesso")
    })
    public ResponseEntity<ApiEnvelope<ReferenceDataCatalogDTO>> getMaterialOptions(HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Catalogos de referencia carregados com sucesso",
                referenceDataService.getMaterialOptions()
        );
    }
}
