package com.ecobook.controller;

import com.ecobook.annotation.RequireCompleteProfile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/solicitacoes")
public class SolicitacaoController {

    @PostMapping
    @RequireCompleteProfile
    public ResponseEntity<Void> createSolicitacao() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
