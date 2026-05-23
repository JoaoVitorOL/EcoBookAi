package com.ecobook.controller;

import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import com.ecobook.dto.ReferenceDataCatalogDTO;
import com.ecobook.service.ReferenceDataService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/reference-data")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    @GetMapping("/material-options")
    public ResponseEntity<ApiEnvelope<ReferenceDataCatalogDTO>> getMaterialOptions(HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Catalogos de referencia carregados com sucesso",
                referenceDataService.getMaterialOptions()
        );
    }
}
