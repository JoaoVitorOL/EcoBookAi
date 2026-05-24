package com.ecobook.service;

import com.ecobook.dto.AuditLogDTO;
import com.ecobook.dto.PagedResponseDTO;
import com.ecobook.model.AuditLog;
import com.ecobook.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Executes the log operation.
     *
     * @param action the action value
     * @param actorUserId the actorUserId value
     * @param actorEmail the actorEmail value
     * @param targetUserId the targetUserId value
     * @param resourceType the resourceType value
     * @param resourceId the resourceId value
     * @param details the details value
     */
    @Transactional
    public void log(String action,
                    UUID actorUserId,
                    String actorEmail,
                    UUID targetUserId,
                    String resourceType,
                    String resourceId,
                    Map<String, String> details) {
        auditLogRepository.save(AuditLog.builder()
                .actorUserId(actorUserId)
                .actorEmail(trimToNull(actorEmail))
                .targetUserId(targetUserId)
                .action(action)
                .resourceType(trimToNull(resourceType))
                .resourceId(trimToNull(resourceId))
                .details(details == null ? new LinkedHashMap<>() : new LinkedHashMap<>(details))
                .build());
    }

    /**
     * Executes the search operation.
     *
     * @param actorUserId the actorUserId value
     * @param targetUserId the targetUserId value
     * @param action the action value
     * @param from the from value
     * @param to the to value
     * @param pageRequest the pageRequest value
     * @return the operation result
     */
    @Transactional(readOnly = true)
    public PagedResponseDTO<AuditLogDTO> search(UUID actorUserId,
                                                UUID targetUserId,
                                                String action,
                                                LocalDateTime from,
                                                LocalDateTime to,
                                                PageRequest pageRequest) {
        Page<AuditLog> page = auditLogRepository.findAll(buildSpecification(actorUserId, targetUserId, action, from, to), pageRequest);
        return PagedResponseDTO.of(
                page.getContent().stream().map(this::toDto).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    /**
     * Lists audit log entries related to the provided user.
     * @param userId user identifier
     * @return requested list
     */
    @Transactional(readOnly = true)
    public java.util.List<AuditLogDTO> listUserRelatedLogs(UUID userId) {
        return auditLogRepository.findByActorUserIdOrTargetUserIdOrderByCreatedAtDesc(userId, userId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Maps an audit-log entity into the API DTO representation.
     * @param auditLog audit-log entity to map
     * @return mapped DTO representation
     */
    public AuditLogDTO toDto(AuditLog auditLog) {
        return AuditLogDTO.builder()
                .id(auditLog.getId().toString())
                .actorUserId(auditLog.getActorUserId() == null ? null : auditLog.getActorUserId().toString())
                .actorEmail(auditLog.getActorEmail())
                .targetUserId(auditLog.getTargetUserId() == null ? null : auditLog.getTargetUserId().toString())
                .action(auditLog.getAction())
                .resourceType(auditLog.getResourceType())
                .resourceId(auditLog.getResourceId())
                .details(auditLog.getDetails())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }

    private Specification<AuditLog> buildSpecification(UUID actorUserId,
                                                       UUID targetUserId,
                                                       String action,
                                                       LocalDateTime from,
                                                       LocalDateTime to) {
        return (root, query, criteriaBuilder) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();

            if (actorUserId != null) {
                predicates.add(criteriaBuilder.equal(root.get("actorUserId"), actorUserId));
            }
            if (targetUserId != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetUserId"), targetUserId));
            }
            if (StringUtils.hasText(action)) {
                predicates.add(criteriaBuilder.equal(root.get("action"), action.trim().toUpperCase(java.util.Locale.ROOT)));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
