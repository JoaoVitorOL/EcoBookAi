package com.ecobook;

import com.ecobook.config.CacheNames;
import com.ecobook.dto.UpdateProfileRequestDTO;
import com.ecobook.dto.UserConsentStatusDTO;
import com.ecobook.dto.UsuarioDTO;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.NecessidadeAcademica;
import com.ecobook.model.enums.Role;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioServiceCacheTest extends BaseIntegrationTest {

    private static final String SEEDED_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHiPpclJ3DgA6hfa/vw/jemjL5V6cO9e";

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        evict(CacheNames.USER_PROFILE);
        evict(CacheNames.USER_CONSENT_STATUS);
        evict(CacheNames.USER_AUTH_CONTEXT);
    }

    @Test
    @DisplayName("getByEmail should serve the cached profile until a successful profile update invalidates it")
    void shouldInvalidateCachedProfileAfterUpdate() {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("cached-profile@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Nome Inicial")
                .perfilCompleto(false)
                .role(Role.USER)
                .build());

        UsuarioDTO firstRead = usuarioService.getByEmail(usuario.getEmail());
        usuario.setNome("Alterado Somente No Banco");
        usuarioRepository.saveAndFlush(usuario);

        UsuarioDTO cachedRead = usuarioService.getByEmail(usuario.getEmail());
        assertThat(cachedRead.getNome()).isEqualTo("Nome Inicial");

        usuarioService.updateProfile(usuario.getEmail(), UpdateProfileRequestDTO.builder()
                .nome("Nome Atualizado")
                .whatsapp("+5511991234567")
                .cpf("52998224725")
                .cidade("Ribeirão Preto")
                .bairro("Jardim Botânico")
                .instituicao("IFSP")
                .consentimentoIa(true)
                .necessidadesAcademicas(Set.of(NecessidadeAcademica.TEXTBOOKS))
                .build());

        UsuarioDTO refreshedRead = usuarioService.getByEmail(usuario.getEmail());
        assertThat(refreshedRead.getNome()).isEqualTo("Nome Atualizado");
        assertThat(refreshedRead.getCidade()).isEqualTo("RIBEIRAO PRETO");
        assertThat(refreshedRead.getBairro()).isEqualTo("Jardim Botânico");
        assertThat(refreshedRead.getConsentimentoIa()).isTrue();

        Cache profileCache = cacheManager.getCache(CacheNames.USER_PROFILE);
        assertThat(profileCache).isNotNull();
        assertThat(profileCache.get(usuario.getEmail())).isNotNull();
        assertThat(firstRead.getNome()).isEqualTo("Nome Inicial");
    }

    @Test
    @DisplayName("getConsentStatus should refresh after updateAiConsent invalidates the cached consent payload")
    void shouldInvalidateCachedConsentStatusAfterAiConsentUpdate() {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("cached-consent@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Consent User")
                .perfilCompleto(false)
                .consentimentoIa(false)
                .role(Role.USER)
                .build());

        UserConsentStatusDTO firstStatus = usuarioService.getConsentStatus(usuario.getEmail());
        assertThat(firstStatus.getAiConsentEnabled()).isFalse();

        usuarioService.updateAiConsent(usuario.getEmail(), true);

        UserConsentStatusDTO refreshedStatus = usuarioService.getConsentStatus(usuario.getEmail());
        assertThat(refreshedStatus.getAiConsentEnabled()).isTrue();
        assertThat(refreshedStatus.getAiConsentGivenAt()).isNotNull();
    }

    private void evict(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
