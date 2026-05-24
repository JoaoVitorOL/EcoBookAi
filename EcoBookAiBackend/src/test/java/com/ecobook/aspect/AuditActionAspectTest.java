package com.ecobook.aspect;

import com.ecobook.annotation.AuditAction;
import com.ecobook.model.Usuario;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.service.AuditLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditActionAspectTest {

    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final AuditActionAspect auditActionAspect = new AuditActionAspect(auditLogService, usuarioRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("around should evaluate SpEL expressions and persist the audit entry for the authenticated actor")
    void shouldPersistAuditEntryForAuthenticatedActor() throws Throwable {
        UUID actorId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        String materialId = "material-123";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("actor@example.com", "n/a")
        );

        Usuario actor = Usuario.builder()
                .id(actorId)
                .email("actor@example.com")
                .build();
        when(usuarioRepository.findByEmailIgnoreCase("actor@example.com")).thenReturn(Optional.of(actor));

        ProceedingJoinPoint joinPoint = joinPointFor(
                "approveMaterial",
                new Class<?>[]{String.class, UUID.class},
                new Object[]{materialId, targetUserId},
                "OK"
        );

        Object result = auditActionAspect.around(joinPoint, annotatedMethod("approveMaterial", String.class, UUID.class).getAnnotation(AuditAction.class));

        assertThat(result).isEqualTo("OK");
        verify(auditLogService).log(
                "MATERIAL_APPROVED",
                actorId,
                "actor@example.com",
                targetUserId,
                "MATERIAL",
                materialId,
                java.util.Map.of()
        );
    }

    @Test
    @DisplayName("around should skip audit persistence when there is no authenticated principal name")
    void shouldSkipAuditWhenPrincipalNameIsBlank() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(" ", "n/a")
        );

        ProceedingJoinPoint joinPoint = joinPointFor(
                "approveMaterial",
                new Class<?>[]{String.class, UUID.class},
                new Object[]{"material-123", UUID.randomUUID()},
                "OK"
        );

        Object result = auditActionAspect.around(joinPoint, annotatedMethod("approveMaterial", String.class, UUID.class).getAnnotation(AuditAction.class));

        assertThat(result).isEqualTo("OK");
        verify(auditLogService, never()).log(eq("MATERIAL_APPROVED"), eq(null), eq(null), eq(null), eq("MATERIAL"), eq("material-123"), eq(java.util.Map.of()));
    }

    @Test
    @DisplayName("around should tolerate unknown actors and invalid UUID expressions")
    void shouldTolerateUnknownActorsAndInvalidTargetIdExpression() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ghost@example.com", "n/a")
        );
        when(usuarioRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

        ProceedingJoinPoint joinPoint = joinPointFor(
                "exportData",
                new Class<?>[]{String.class},
                new Object[]{"export-123"},
                "export-123"
        );

        Object result = auditActionAspect.around(joinPoint, annotatedMethod("exportData", String.class).getAnnotation(AuditAction.class));

        assertThat(result).isEqualTo("export-123");
        verify(auditLogService).log(
                "USER_EXPORT",
                null,
                "ghost@example.com",
                null,
                "USER",
                "export-123",
                java.util.Map.of()
        );
    }

    private ProceedingJoinPoint joinPointFor(String methodName,
                                             Class<?>[] parameterTypes,
                                             Object[] args,
                                             Object result) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(annotatedMethod(methodName, parameterTypes));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(result);
        return joinPoint;
    }

    private Method annotatedMethod(String name, Class<?>... parameterTypes) {
        try {
            return AuditFixture.class.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @SuppressWarnings("unused")
    private static class AuditFixture {

        @AuditAction(
                action = "MATERIAL_APPROVED",
                resourceType = "MATERIAL",
                resourceIdExpression = "#materialId",
                targetUserIdExpression = "#targetUserId"
        )
        public String approveMaterial(String materialId, UUID targetUserId) {
            return "OK";
        }

        @AuditAction(
                action = "USER_EXPORT",
                resourceType = "USER",
                resourceIdExpression = "#result",
                targetUserIdExpression = "'not-a-uuid'"
        )
        public String exportData(String exportId) {
            return exportId;
        }
    }
}
