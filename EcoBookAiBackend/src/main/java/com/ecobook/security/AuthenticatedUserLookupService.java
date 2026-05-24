package com.ecobook.security;

import com.ecobook.config.CacheNames;
import com.ecobook.exception.ResourceNotFoundException;
import com.ecobook.model.Usuario;
import com.ecobook.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cached lookup for authenticated request metadata used by security components.
 */
@Service
@RequiredArgsConstructor
public class AuthenticatedUserLookupService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Loads the authenticated-user snapshot required by security-sensitive flows.
     * @param email authenticated user email
     * @return loaded value
     */
    @Cacheable(value = CacheNames.USER_AUTH_CONTEXT, key = "#email", sync = true)
    @Transactional(readOnly = true)
    public AuthenticatedUserSnapshot loadRequiredByEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        return new AuthenticatedUserSnapshot(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.isPerfilCompleto()
        );
    }
}
