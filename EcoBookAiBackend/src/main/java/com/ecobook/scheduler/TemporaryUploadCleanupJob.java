package com.ecobook.scheduler;

import com.ecobook.model.TemporaryUpload;
import com.ecobook.repository.TemporaryUploadRepository;
import com.ecobook.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TemporaryUploadCleanupJob {

    private final TemporaryUploadRepository temporaryUploadRepository;
    private final ImageStorageService imageStorageService;

    @Scheduled(fixedDelay = 21600000)
    public void cleanupExpiredUploads() {
        List<TemporaryUpload> expiredUploads = temporaryUploadRepository
                .findByExpiresAtBeforeAndMaterialIsNull(LocalDateTime.now());

        expiredUploads.forEach(upload -> imageStorageService.deleteIfExists(upload.getFilePath()));
        temporaryUploadRepository.deleteAll(expiredUploads);

        if (!expiredUploads.isEmpty()) {
            log.info("Cleaned up {} temporary uploads", expiredUploads.size());
        }
    }
}
