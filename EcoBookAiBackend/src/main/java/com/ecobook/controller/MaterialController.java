package com.ecobook.controller;

import com.ecobook.annotation.RequireCompleteProfile;
import com.ecobook.dto.CreateMaterialRequestDTO;
import com.ecobook.dto.GeminiResponseDTO;
import com.ecobook.dto.MaterialDTO;
import com.ecobook.service.MaterialService;
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
    public ResponseEntity<GeminiResponseDTO> previewMaterial(Authentication authentication,
                                                             @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(materialService.previewMaterial(authentication.getName(), file));
    }

    @PostMapping
    @RequireCompleteProfile
    public ResponseEntity<MaterialDTO> createMaterial(Authentication authentication,
                                                      @RequestBody CreateMaterialRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materialService.createMaterial(authentication.getName(), request));
    }

    @PutMapping("/{id}")
    @RequireCompleteProfile
    public ResponseEntity<Void> updateMaterial(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @DeleteMapping("/{id}")
    @RequireCompleteProfile
    public ResponseEntity<Void> deleteMaterial(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
