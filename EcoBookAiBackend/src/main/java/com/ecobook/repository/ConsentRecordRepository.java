package com.ecobook.repository;

import com.ecobook.model.ConsentRecord;
import com.ecobook.model.enums.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, UUID> {
    List<ConsentRecord> findByUserIdOrderByCreatedAtAsc(UUID userId);
    Optional<ConsentRecord> findFirstByUserIdAndConsentTypeOrderByCreatedAtDesc(UUID userId, ConsentType consentType);
}
