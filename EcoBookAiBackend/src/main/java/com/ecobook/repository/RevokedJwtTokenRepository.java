package com.ecobook.repository;

import com.ecobook.model.RevokedJwtToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface RevokedJwtTokenRepository extends JpaRepository<RevokedJwtToken, UUID> {
    boolean existsByTokenHash(String tokenHash);
    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
