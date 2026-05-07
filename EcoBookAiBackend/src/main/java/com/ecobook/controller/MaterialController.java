package com.ecobook.controller;

import com.ecobook.annotation.RequireCompleteProfile;
import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import com.ecobook.dto.CreateMaterialRequestDTO;
import com.ecobook.dto.GeminiResponseDTO;
import com.ecobook.dto.MaterialDTO;
import com.ecobook.service.MaterialService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/materiais")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @PostMapping("/preview")
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<GeminiResponseDTO>> previewMaterial(Authentication authentication,
                                                                          @RequestPart("file") MultipartFile file,
                                                                          HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Preview do material gerado com sucesso",
                materialService.previewMaterial(authentication.getName(), file)
        );
    }

    @PostMapping
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<MaterialDTO>> createMaterial(Authentication authentication,
                                                                   @RequestBody CreateMaterialRequestDTO request,
                                                                   HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.created(
                servletRequest,
                "Material publicado com sucesso",
                materialService.createMaterial(authentication.getName(), request)
        );
    }

    @PutMapping("/{id}")
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<Void>> updateMaterial(@PathVariable String id,
                                                            HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.status(
                HttpStatus.NOT_IMPLEMENTED,
                servletRequest,
                "Atualizacao de materiais sera implementada nas proximas fases"
        );
    }

    @DeleteMapping("/{id}")
    @RequireCompleteProfile
    public ResponseEntity<ApiEnvelope<Void>> deleteMaterial(@PathVariable String id,
                                                            HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.status(
                HttpStatus.NOT_IMPLEMENTED,
                servletRequest,
                "Remocao de materiais sera implementada nas proximas fases"
        );
    }
}
