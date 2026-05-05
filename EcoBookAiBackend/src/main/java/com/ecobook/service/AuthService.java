package com.ecobook.service;

import com.ecobook.dto.AuthResponseDTO;
import com.ecobook.dto.LoginRequestDTO;
import com.ecobook.dto.RegisterRequestDTO;
import com.ecobook.exception.ConflictException;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Role;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Authentication service for local email/password registration and login.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedNome = normalizeNome(request.getNome());

        if (usuarioRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new ConflictException("Este email ja esta cadastrado");
        }

        Usuario usuario = createUser(normalizedEmail, normalizedNome, request.getPassword());

        usuario.refreshPerfilCompleto();
        Usuario savedUser = usuarioRepository.save(usuario);
        return buildAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(this::invalidCredentials);

        if (!StringUtils.hasText(usuario.getPasswordHash()) ||
                !passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw invalidCredentials();
        }

        usuario.refreshPerfilCompleto();
        return buildAuthResponse(usuario);
    }

    private Usuario createUser(String email, String nome, String rawPassword) {
        return Usuario.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .nome(nome)
                .role(Role.USER)
                .perfilCompleto(false)
                .consentimentoIa(false)
                .build();
    }

    private AuthResponseDTO buildAuthResponse(Usuario usuario) {
        String token = jwtTokenProvider.generateToken(
                usuario.getEmail(),
                usuario.getRole().name(),
                usuario.isPerfilCompleto(),
                usuario.getId().toString()
        );

        return AuthResponseDTO.builder()
                .id(usuario.getId().toString())
                .email(usuario.getEmail())
                .nome(usuario.getNome())
                .whatsapp(usuario.getWhatsapp())
                .cidade(usuario.getCidade())
                .bairro(usuario.getBairro())
                .instituicao(usuario.getInstituicao())
                .perfilCompleto(usuario.isPerfilCompleto())
                .consentimentoIa(Boolean.TRUE.equals(usuario.getConsentimentoIa()))
                .role(usuario.getRole().name())
                .token(token)
                .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNome(String nome) {
        return nome.trim();
    }

    private BadCredentialsException invalidCredentials() {
        return new BadCredentialsException("Email ou senha invalidos");
    }
}
