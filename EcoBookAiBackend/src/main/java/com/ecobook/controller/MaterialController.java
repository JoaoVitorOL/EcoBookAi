package com.ecobook.controller;

import com.ecobook.annotation.RequireCompleteProfile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/materiais")
public class MaterialController {

    @PostMapping("/preview")
    @RequireCompleteProfile
    public ResponseEntity<Void> previewMaterial() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PostMapping
    @RequireCompleteProfile
    public ResponseEntity<Void> createMaterial() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
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
