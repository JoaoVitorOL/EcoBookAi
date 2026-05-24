package com.ecobook.service;

import com.ecobook.model.RevokedJwtToken;
import com.ecobook.repository.RevokedJwtTokenRepository;
import com.ecobook.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TokenRevocationServiceTest {

    private final RevokedJwtTokenRepository revokedJwtTokenRepository = mock(RevokedJwtTokenRepository.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final TokenRevocationService tokenRevocationService = new TokenRevocationService(
            revokedJwtTokenRepository,
            jwtTokenProvider
    );

    @Test
    @DisplayName("revoke should ignore blank tokens")
    void shouldIgnoreBlankToken() {
        tokenRevocationService.revoke("  ", UUID.randomUUID());

        verifyNoInteractions(jwtTokenProvider, revokedJwtTokenRepository);
    }

    @Test
    @DisplayName("revoke should not save the token twice when the hash already exists")
    void shouldSkipExistingTokenHash() {
        String token = "header.payload.signature";

        when(jwtTokenProvider.getExpiration(token)).thenReturn(Date.from(Instant.parse("2026-05-23T12:00:00Z")));
        when(revokedJwtTokenRepository.existsByTokenHash(anyString())).thenReturn(true);

        tokenRevocationService.revoke(token, UUID.randomUUID());

        verify(jwtTokenProvider).getExpiration(token);
        verify(revokedJwtTokenRepository).existsByTokenHash(anyString());
        verify(revokedJwtTokenRepository, never()).save(any(RevokedJwtToken.class));
    }

    @Test
    @DisplayName("revoke should persist a new revoked token hash with the JWT expiration")
    void shouldPersistRevokedToken() {
        UUID userId = UUID.randomUUID();
        String token = "token-to-revoke";

        when(jwtTokenProvider.getExpiration(token)).thenReturn(Date.from(Instant.parse("2026-05-24T15:30:00Z")));
        when(revokedJwtTokenRepository.existsByTokenHash(anyString())).thenReturn(false);

        tokenRevocationService.revoke(token, userId);

        verify(revokedJwtTokenRepository).save(argThat(revokedToken ->
                revokedToken.getUserId().equals(userId)
                        && revokedToken.getTokenHash() != null
                        && !revokedToken.getTokenHash().isBlank()
                        && revokedToken.getExpiresAt() != null
        ));
    }

    @Test
    @DisplayName("isRevoked should return false for blank tokens and delegate to the repository otherwise")
    void shouldReportRevocationStatus() {
        when(revokedJwtTokenRepository.existsByTokenHash(anyString())).thenReturn(true);

        assertThat(tokenRevocationService.isRevoked("")).isFalse();
        assertThat(tokenRevocationService.isRevoked("token-to-check")).isTrue();
    }
}
