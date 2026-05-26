package com.ecobook.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SolicitacaoTest {

    @Test
    @DisplayName("hasExpired should treat the exact expiry instant as expired")
    void shouldExpireAtExactBoundaryMoment() {
        LocalDateTime boundary = LocalDateTime.now();
        Solicitacao solicitacao = Solicitacao.builder()
                .expiresAt(boundary)
                .build();

        assertThat(solicitacao.hasExpired(boundary)).isTrue();
    }

    @Test
    @DisplayName("hasExpired should stay false before the expiry instant")
    void shouldNotExpireBeforeBoundary() {
        LocalDateTime boundary = LocalDateTime.now().plusSeconds(1);
        Solicitacao solicitacao = Solicitacao.builder()
                .expiresAt(boundary)
                .build();

        assertThat(solicitacao.hasExpired(boundary.minusNanos(1))).isFalse();
    }
}
