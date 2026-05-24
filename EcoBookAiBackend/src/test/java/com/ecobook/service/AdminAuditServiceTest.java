package com.ecobook.service;

import com.ecobook.dto.AuditLogDTO;
import com.ecobook.dto.PagedResponseDTO;
import com.ecobook.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAuditServiceTest {

    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final AdminAuditService adminAuditService = new AdminAuditService(auditLogService);

    @Test
    @DisplayName("listAuditLogs should parse UUID filters and delegate to AuditLogService")
    void shouldDelegateWithParsedUuidFilters() {
        UUID actorUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        PageRequest pageRequest = PageRequest.of(0, 20);
        PagedResponseDTO<AuditLogDTO> response = PagedResponseDTO.of(List.of(), 0, 20, 0);

        when(auditLogService.search(eq(actorUserId), eq(targetUserId), eq("USER_EXPORT"), eq(null), eq(null), eq(pageRequest)))
                .thenReturn(response);

        assertThat(adminAuditService.listAuditLogs(
                actorUserId.toString(),
                targetUserId.toString(),
                "USER_EXPORT",
                null,
                null,
                pageRequest
        )).isSameAs(response);

        verify(auditLogService).search(actorUserId, targetUserId, "USER_EXPORT", null, null, pageRequest);
    }

    @Test
    @DisplayName("listAuditLogs should reject invalid UUID filters with field-level details")
    void shouldRejectInvalidUuidFilters() {
        assertThatThrownBy(() -> adminAuditService.listAuditLogs(
                "not-a-uuid",
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20)
        ))
                .isInstanceOf(BadRequestException.class)
                .satisfies(exception -> {
                    BadRequestException badRequestException = (BadRequestException) exception;
                    assertThat(badRequestException.getFieldErrors())
                            .containsKey("actor_user_id");
                    assertThat(badRequestException.getFieldErrors().get("actor_user_id"))
                            .contains("UUID");
                });
    }
}
