package com.ecobook.service;

import com.ecobook.dto.ConsentRecordDTO;
import com.ecobook.dto.UserConsentStatusDTO;
import com.ecobook.model.ConsentRecord;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.ConsentStatus;
import com.ecobook.model.enums.ConsentType;
import com.ecobook.repository.ConsentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsentService {

    private final ConsentRecordRepository consentRecordRepository;
    private final AuditLogService auditLogService;

    /**
     * Records that the user accepted the platform terms and privacy consent.
     * @param usuario user entity involved in the operation
     */
    @Transactional
    public void recordPlatformConsent(Usuario usuario) {
        if (consentRecordRepository.findFirstByUserIdAndConsentTypeOrderByCreatedAtDesc(usuario.getId(), ConsentType.PLATFORM)
                .filter(record -> record.getStatus() == ConsentStatus.GIVEN)
                .isPresent()) {
            return;
        }

        consentRecordRepository.save(ConsentRecord.builder()
                .userId(usuario.getId())
                .consentType(ConsentType.PLATFORM)
                .status(ConsentStatus.GIVEN)
                .build());

        auditLogService.log(
                "PLATFORM_CONSENT_GIVEN",
                usuario.getId(),
                usuario.getEmail(),
                usuario.getId(),
                "CONSENT",
                ConsentType.PLATFORM.name(),
                Map.of()
        );
    }

    /**
     * Records a change in the user's AI-consent status.
     * @param usuario user entity involved in the operation
     * @param enabled new AI-consent status
     */
    @Transactional
    public void recordAiConsentChange(Usuario usuario, boolean enabled) {
        UUID userId = usuario.getId();
        LocalDateTime now = LocalDateTime.now();

        if (enabled) {
            consentRecordRepository.save(ConsentRecord.builder()
                    .userId(userId)
                    .consentType(ConsentType.AI_CLASSIFICATION)
                    .status(ConsentStatus.GIVEN)
                    .createdAt(now)
                    .build());

            auditLogService.log(
                    "AI_CONSENT_GIVEN",
                    userId,
                    usuario.getEmail(),
                    userId,
                    "CONSENT",
                    ConsentType.AI_CLASSIFICATION.name(),
                    Map.of()
            );
            return;
        }

        consentRecordRepository.findFirstByUserIdAndConsentTypeOrderByCreatedAtDesc(userId, ConsentType.AI_CLASSIFICATION)
                .filter(record -> record.getStatus() == ConsentStatus.GIVEN && record.getRevokedAt() == null)
                .ifPresent(record -> {
                    record.setRevokedAt(now);
                    consentRecordRepository.save(record);
                });

        consentRecordRepository.save(ConsentRecord.builder()
                .userId(userId)
                .consentType(ConsentType.AI_CLASSIFICATION)
                .status(ConsentStatus.REVOKED)
                .createdAt(now)
                .revokedAt(now)
                .build());

        auditLogService.log(
                "AI_CONSENT_REVOKED",
                userId,
                usuario.getEmail(),
                userId,
                "CONSENT",
                ConsentType.AI_CLASSIFICATION.name(),
                Map.of()
        );
    }

    /**
     * Records the revocation of the user's platform consent.
     * @param usuario user entity involved in the operation
     */
    @Transactional
    public void recordPlatformRevocation(Usuario usuario) {
        UUID userId = usuario.getId();
        LocalDateTime now = LocalDateTime.now();

        consentRecordRepository.findFirstByUserIdAndConsentTypeOrderByCreatedAtDesc(userId, ConsentType.PLATFORM)
                .filter(record -> record.getStatus() == ConsentStatus.GIVEN && record.getRevokedAt() == null)
                .ifPresent(record -> {
                    record.setRevokedAt(now);
                    consentRecordRepository.save(record);
                });

        consentRecordRepository.save(ConsentRecord.builder()
                .userId(userId)
                .consentType(ConsentType.PLATFORM)
                .status(ConsentStatus.REVOKED)
                .createdAt(now)
                .revokedAt(now)
                .build());
    }

    /**
     * Executes the get consent status operation.
     * @param usuario user entity involved in the operation
     * @return requested value
     */
    @Transactional(readOnly = true)
    public UserConsentStatusDTO getConsentStatus(Usuario usuario) {
        List<ConsentRecord> history = consentRecordRepository.findByUserIdOrderByCreatedAtAsc(usuario.getId());
        ConsentRecord lastPlatformGiven = latest(history, ConsentType.PLATFORM, ConsentStatus.GIVEN);
        ConsentRecord lastAiGiven = latest(history, ConsentType.AI_CLASSIFICATION, ConsentStatus.GIVEN);
        ConsentRecord lastAiRevoked = latest(history, ConsentType.AI_CLASSIFICATION, ConsentStatus.REVOKED);

        return UserConsentStatusDTO.builder()
                .platformConsentGiven(lastPlatformGiven != null)
                .platformConsentGivenAt(lastPlatformGiven == null ? null : lastPlatformGiven.getCreatedAt())
                .aiConsentEnabled(Boolean.TRUE.equals(usuario.getConsentimentoIa()))
                .aiConsentGivenAt(lastAiGiven == null ? null : lastAiGiven.getCreatedAt())
                .aiConsentRevokedAt(lastAiRevoked == null ? null : lastAiRevoked.getRevokedAt())
                .history(history.stream().map(this::toDto).toList())
                .build();
    }

    /**
     * Lists the recorded consent history for the provided user.
     * @param userId user identifier
     * @return requested value
     */
    @Transactional(readOnly = true)
    public List<ConsentRecordDTO> getConsentHistory(UUID userId) {
        return consentRecordRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    private ConsentRecordDTO toDto(ConsentRecord record) {
        return ConsentRecordDTO.builder()
                .id(record.getId().toString())
                .consentType(record.getConsentType().name())
                .status(record.getStatus().name())
                .createdAt(record.getCreatedAt())
                .revokedAt(record.getRevokedAt())
                .build();
    }

    private ConsentRecord latest(List<ConsentRecord> records, ConsentType type, ConsentStatus status) {
        return records.stream()
                .filter(record -> record.getConsentType() == type && record.getStatus() == status)
                .reduce((left, right) -> right)
                .orElse(null);
    }
}
