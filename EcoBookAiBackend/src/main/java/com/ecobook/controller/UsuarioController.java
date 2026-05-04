package com.ecobook.controller;

import com.ecobook.dto.UpdateProfileRequestDTO;
import com.ecobook.dto.UsuarioDTO;
import com.ecobook.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UsuarioDTO> getMe(Authentication authentication) {
        return ResponseEntity.ok(usuarioService.getByEmail(authentication.getName()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UsuarioDTO> updateMe(Authentication authentication,
                                               @Valid @RequestBody UpdateProfileRequestDTO request) {
        return ResponseEntity.ok(usuarioService.updateProfile(authentication.getName(), request));
    }
}
