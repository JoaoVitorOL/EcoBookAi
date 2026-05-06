package com.ecobook.repository;

import com.ecobook.model.TemporaryUpload;
import com.ecobook.model.enums.UploadProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemporaryUploadRepository extends JpaRepository<TemporaryUpload, UUID> {
    Optional<TemporaryUpload> findByUploadId(String uploadId);

    List<TemporaryUpload> findByExpiresAtBeforeAndMaterialIsNull(LocalDateTime expiresAt);

    long countByStatus(UploadProcessingStatus status);
}
