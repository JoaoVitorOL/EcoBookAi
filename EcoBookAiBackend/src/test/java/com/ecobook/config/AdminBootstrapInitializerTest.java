package com.ecobook.config;

import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Role;
import com.ecobook.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapInitializerTest {

    private final AdminBootstrapProperties properties = new AdminBootstrapProperties();
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AdminBootstrapInitializer initializer = new AdminBootstrapInitializer(
            properties,
            usuarioRepository,
            passwordEncoder
    );

    @Test
    @DisplayName("run should promote an existing user to ADMIN when bootstrap is enabled")
    void shouldPromoteExistingUser() throws Exception {
        Usuario existingUser = Usuario.builder()
                .id(UUID.randomUUID())
                .email("moderator@example.com")
                .passwordHash("hash")
                .nome("Moderador")
                .role(Role.USER)
                .build();

        properties.setEnabled(true);
        properties.setEmail("moderator@example.com");
        when(usuarioRepository.findByEmailIgnoreCase("moderator@example.com")).thenReturn(Optional.of(existingUser));

        initializer.run(mock(ApplicationArguments.class));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("run should create an admin account when the email does not exist and a password is configured")
    void shouldCreateAdminWhenMissing() throws Exception {
        properties.setEnabled(true);
        properties.setEmail("admin-bootstrap@example.com");
        properties.setPassword("SenhaAdmin123");
        properties.setNome("Admin Bootstrap");

        when(usuarioRepository.findByEmailIgnoreCase("admin-bootstrap@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("SenhaAdmin123")).thenReturn("encoded-password");

        initializer.run(mock(ApplicationArguments.class));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("admin-bootstrap@example.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded-password");
        assertThat(captor.getValue().getNome()).isEqualTo("Admin Bootstrap");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ADMIN);
    }
}
