package com.ecobook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AuditLogDTO {
    private String id;
    private String actorUserId;
    private String actorEmail;
    private String targetUserId;
    private String action;
    private String resourceType;
    private String resourceId;
    private Map<String, String> details;
    private LocalDateTime createdAt;
}
