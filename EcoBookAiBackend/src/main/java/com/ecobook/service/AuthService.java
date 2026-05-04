package com.ecobook.service;

import com.ecobook.dto.AuthResponseDTO;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Role;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Authentication service for user registration and login
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtTokenProvider jwtTokenProvider;
    @Qualifier("googleJwtDecoder")
    private final JwtDecoder googleJwtDecoder;

    /**
     * Register or login user with Google OAuth2 token
     */
    @Transactional
    public AuthResponseDTO registerOrLoginUser(String googleToken) {
        Jwt googleJwt = decodeGoogleToken(googleToken);

        String googleId = googleJwt.getSubject();
        String email = googleJwt.getClaimAsString("email");
        String name = googleJwt.getClaimAsString("name");
        Boolean emailVerified = googleJwt.getClaimAsBoolean("email_verified");

        if (!StringUtils.hasText(googleId) || !StringUtils.hasText(email)) {
            throw new BadCredentialsException("Google token is missing required identity claims");
        }

        if (emailVerified != null && !emailVerified) {
            throw new BadCredentialsException("Google account email must be verified");
        }

        Usuario usuario = usuarioRepository.findByGoogleId(googleId)
                .or(() -> usuarioRepository.findByEmailIgnoreCase(email))
                .map(existing -> mergeGoogleIdentity(existing, googleId, email, name))
                .orElseGet(() -> createUser(googleId, email, name));

        usuario.refreshPerfilCompleto();
        Usuario savedUser = usuarioRepository.save(usuario);
        String token = jwtTokenProvider.generateToken(
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.isPerfilCompleto(),
                savedUser.getId().toString()
        );

        return AuthResponseDTO.builder()
                .id(savedUser.getId().toString())
                .email(savedUser.getEmail())
                .nome(savedUser.getNome())
                .perfilCompleto(savedUser.getPerfilCompleto())
                .role(savedUser.getRole().name())
                .token(token)
                .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                .build();
    }

    private Jwt decodeGoogleToken(String googleToken) {
        try {
            return googleJwtDecoder.decode(googleToken);
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid Google token", ex);
        }
    }

    private Usuario createUser(String googleId, String email, String name) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String resolvedName = StringUtils.hasText(name) ? name.trim() : normalizedEmail.substring(0, normalizedEmail.indexOf('@'));

        return Usuario.builder()
                .email(normalizedEmail)
                .nome(resolvedName)
                .googleId(googleId)
                .role(Role.USER)
                .perfilCompleto(false)
                .consentimentoIa(false)
                .build();
    }

    private Usuario mergeGoogleIdentity(Usuario existingUser, String googleId, String email, String name) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        existingUser.setEmail(normalizedEmail);

        if (!StringUtils.hasText(existingUser.getGoogleId())) {
            existingUser.setGoogleId(googleId);
        } else if (!existingUser.getGoogleId().equals(googleId)) {
            throw new BadCredentialsException("Google token does not match the registered account");
        }

        if (!StringUtils.hasText(existingUser.getNome()) && StringUtils.hasText(name)) {
            existingUser.setNome(name.trim());
        }

        return existingUser;
    }
}
