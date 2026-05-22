package com.ecobook.service;

import com.ecobook.model.RevokedJwtToken;
import com.ecobook.repository.RevokedJwtTokenRepository;
import com.ecobook.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final RevokedJwtTokenRepository revokedJwtTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void revoke(String rawToken, UUID userId) {
        if (!StringUtils.hasText(rawToken)) {
            return;
        }

        LocalDateTime expiresAt = jwtTokenProvider.getExpiration(rawToken)
                .toInstant()
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime();

        String tokenHash = hash(rawToken);
        if (revokedJwtTokenRepository.existsByTokenHash(tokenHash)) {
            return;
        }

        revokedJwtTokenRepository.save(RevokedJwtToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build());
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return false;
        }
        return revokedJwtTokenRepository.existsByTokenHash(hash(rawToken));
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available in this runtime", exception);
        }
    }
}
