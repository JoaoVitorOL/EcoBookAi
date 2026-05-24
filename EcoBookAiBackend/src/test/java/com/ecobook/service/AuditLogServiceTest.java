package com.ecobook.service;

import com.ecobook.dto.AuditLogDTO;
import com.ecobook.dto.PagedResponseDTO;
import com.ecobook.model.AuditLog;
import com.ecobook.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogServiceTest {

    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final AuditLogService auditLogService = new AuditLogService(auditLogRepository);

    @Test
    @DisplayName("log should trim optional values and default details to an empty map")
    void shouldTrimValuesWhenLogging() {
        UUID actorUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditLogService.log(
                "USER_EXPORT",
                actorUserId,
                "  actor@example.com  ",
                targetUserId,
                "  USER  ",
                "  resource-123  ",
                null
        );

        verify(auditLogRepository).save(any(AuditLog.class));
        AuditLog saved = captureSavedAuditLog();
        assertThat(saved.getActorUserId()).isEqualTo(actorUserId);
        assertThat(saved.getActorEmail()).isEqualTo("actor@example.com");
        assertThat(saved.getTargetUserId()).isEqualTo(targetUserId);
        assertThat(saved.getResourceType()).isEqualTo("USER");
        assertThat(saved.getResourceId()).isEqualTo("resource-123");
        assertThat(saved.getDetails()).isEmpty();
    }

    @Test
    @DisplayName("search should map repository results into a paged DTO")
    void shouldMapSearchResults() {
        UUID actorUserId = UUID.randomUUID();
        PageRequest pageRequest = PageRequest.of(0, 20);
        AuditLog auditLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .actorUserId(actorUserId)
                .actorEmail("actor@example.com")
                .action("USER_EXPORT")
                .resourceType("USER")
                .resourceId("resource-123")
                .details(Map.of("source", "profile"))
                .createdAt(LocalDateTime.now())
                .build();

        when(auditLogRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageRequest)))
                .thenReturn(new PageImpl<>(List.of(auditLog), pageRequest, 1));

        PagedResponseDTO<AuditLogDTO> response = auditLogService.search(
                actorUserId,
                null,
                " user_export ",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                pageRequest
        );

        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getActorEmail()).isEqualTo("actor@example.com");
        assertThat(response.getResults().get(0).getAction()).isEqualTo("USER_EXPORT");
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("listUserRelatedLogs should reuse the DTO mapper for actor and target matches")
    void shouldListUserRelatedLogs() {
        UUID userId = UUID.randomUUID();
        AuditLog actorLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .actorUserId(userId)
                .action("USER_EXPORT")
                .details(Map.of("scope", "self"))
                .createdAt(LocalDateTime.now())
                .build();

        when(auditLogRepository.findByActorUserIdOrTargetUserIdOrderByCreatedAtDesc(userId, userId))
                .thenReturn(List.of(actorLog));

        List<AuditLogDTO> logs = auditLogService.listUserRelatedLogs(userId);

        assertThat(logs).singleElement().satisfies(dto -> {
            assertThat(dto.getActorUserId()).isEqualTo(userId.toString());
            assertThat(dto.getAction()).isEqualTo("USER_EXPORT");
            assertThat(dto.getDetails()).containsEntry("scope", "self");
        });
    }

    private AuditLog captureSavedAuditLog() {
        org.mockito.ArgumentCaptor<AuditLog> captor = org.mockito.ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }
}
