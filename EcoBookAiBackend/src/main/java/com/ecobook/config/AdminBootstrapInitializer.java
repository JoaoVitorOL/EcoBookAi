package com.ecobook.config;

import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Role;
import com.ecobook.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapInitializer implements ApplicationRunner {

    private final AdminBootstrapProperties properties;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        String normalizedEmail = normalizeEmail(properties.getEmail());
        if (normalizedEmail == null) {
            log.warn("Admin bootstrap ignorado: configure admin.bootstrap.email quando enabled=true");
            return;
        }

        usuarioRepository.findByEmailIgnoreCase(normalizedEmail).ifPresentOrElse(existingAdmin -> {
            if (existingAdmin.getRole() == Role.ADMIN) {
                log.info("Admin bootstrap: usuário {} já possui role ADMIN", normalizedEmail);
                return;
            }

            existingAdmin.setRole(Role.ADMIN);
            usuarioRepository.save(existingAdmin);
            log.info("Admin bootstrap: usuário {} promovido para ADMIN", normalizedEmail);
        }, () -> createAdminIfConfigured(normalizedEmail));
    }

    private void createAdminIfConfigured(String normalizedEmail) {
        String password = trimToNull(properties.getPassword());
        if (password == null) {
            log.warn("Admin bootstrap ignorado: usuário {} não existe e nenhum admin.bootstrap.password foi informado", normalizedEmail);
            return;
        }

        String nome = trimToNull(properties.getNome());
        Usuario admin = Usuario.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(password))
                .nome(nome != null ? nome : "Administrador EcoBook")
                .perfilCompleto(false)
                .consentimentoIa(false)
                .role(Role.ADMIN)
                .build();

        usuarioRepository.save(admin);
        log.info("Admin bootstrap: conta ADMIN criada para {}", normalizedEmail);
    }

    private String normalizeEmail(String email) {
        String trimmed = trimToNull(email);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
