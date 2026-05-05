package com.ecobook.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String TEST_SECRET =
            "test-secret-key-for-testing-only-and-long-enough-for-hmac-sha512-signing";

    @Test
    void shouldGenerateAndValidateToken() {
        JwtTokenProvider provider = buildProvider(3_600_000L);

        String token = provider.generateToken("user@example.com", "USER", false, "123");

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getEmailFromToken(token)).isEqualTo("user@example.com");
        assertThat(provider.getRoleFromToken(token)).isEqualTo("USER");
        assertThat(provider.getPerfilCompletoFromToken(token)).isFalse();
    }

    @Test
    void shouldMarkExpiredTokenAsInvalid() throws InterruptedException {
        JwtTokenProvider provider = buildProvider(5L);
        String token = provider.generateToken("expired@example.com", "USER", false, "123");

        Thread.sleep(20L);

        assertThat(provider.validateToken(token)).isFalse();
        assertThat(provider.isTokenExpired(token)).isTrue();
    }

    private JwtTokenProvider buildProvider(long expirationMillis) {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(provider, "jwtExpiration", expirationMillis);
        return provider;
    }
}
