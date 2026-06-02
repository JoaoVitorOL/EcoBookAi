package com.ecobook.service;

import com.ecobook.dto.UpdateProfileRequestDTO;
import com.ecobook.dto.UsuarioDTO;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Role;
import com.ecobook.repository.AuditLogRepository;
import com.ecobook.repository.ConsentRecordRepository;
import com.ecobook.repository.UsuarioRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceUnitTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ImageStorageService imageStorageService;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        AuditLogService auditLogService = new AuditLogService(org.mockito.Mockito.mock(AuditLogRepository.class));
        ConsentService consentService = new ConsentService(org.mockito.Mockito.mock(ConsentRecordRepository.class), auditLogService);
        usuarioService = new UsuarioService(
                usuarioRepository,
                new GeoNormalizationService(),
                validator,
                eventPublisher,
                consentService,
                imageStorageService
        );
    }

    @Test
    @DisplayName("updateProfile should accept arbitrary city text and preserve neighborhood accentuation")
    void shouldAcceptFreeTextCityAndNormalizeIt() {
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hash")
                .nome("Pessoa")
                .perfilCompleto(false)
                .role(Role.USER)
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .nome("Pessoa")
                .whatsapp("+5511991234567")
                .cpf("52998224725")
                .cidade(" Ribeirão Preto ")
                .bairro(" Jardim Botânico ")
                .build();

        UsuarioDTO response = usuarioService.updateProfile("user@example.com", request);

        assertThat(response.getCidade()).isEqualTo("RIBEIRAO PRETO");
        assertThat(response.getBairro()).isEqualTo("Jardim Botânico");
        assertThat(response.getPerfilCompleto()).isTrue();
        verify(usuarioRepository).save(argThat(hasNormalizedGeo("RIBEIRAO PRETO", "Jardim Botânico")));
    }

    private ArgumentMatcher<Usuario> hasNormalizedGeo(String cidade, String bairro) {
        return usuario -> cidade.equals(usuario.getCidade()) && bairro.equals(usuario.getBairro());
    }
}
