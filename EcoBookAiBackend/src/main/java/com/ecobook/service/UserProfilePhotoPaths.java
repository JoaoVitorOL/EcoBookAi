package com.ecobook.service;

import com.ecobook.model.Usuario;
import org.springframework.util.StringUtils;

import java.util.UUID;

public final class UserProfilePhotoPaths {

    private UserProfilePhotoPaths() {
    }

    public static String resolveUrl(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return resolveUrl(usuario.getId(), usuario.getFotoPerfilPath());
    }

    public static String resolveUrl(UUID userId, String storedPath) {
        if (userId == null || !StringUtils.hasText(storedPath)) {
            return null;
        }
        return "/api/v1/usuarios/" + userId + "/foto-perfil";
    }
}
