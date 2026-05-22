package com.ecobook.service;

import com.ecobook.dto.AuditLogDTO;
import com.ecobook.dto.PagedResponseDTO;
import com.ecobook.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public PagedResponseDTO<AuditLogDTO> listAuditLogs(String actorUserId,
                                                       String targetUserId,
                                                       String action,
                                                       LocalDateTime from,
                                                       LocalDateTime to,
                                                       PageRequest pageRequest) {
        return auditLogService.search(
                parseNullableUuid(actorUserId, "actor_user_id"),
                parseNullableUuid(targetUserId, "target_user_id"),
                action,
                from,
                to,
                pageRequest
        );
    }

    private UUID parseNullableUuid(String rawValue, String fieldName) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }

        try {
            return UUID.fromString(rawValue.trim());
        } catch (IllegalArgumentException exception) {
            LinkedHashMap<String, String> fieldErrors = new LinkedHashMap<>();
            fieldErrors.put(fieldName, "Informe um UUID válido");
            throw new BadRequestException("Os filtros de auditoria são inválidos", fieldErrors);
        }
    }
}
