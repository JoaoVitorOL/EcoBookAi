package com.ecobook.aspect;

import com.ecobook.annotation.AuditAction;
import com.ecobook.model.Usuario;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditActionAspect {

    private final AuditLogService auditLogService;
    private final UsuarioRepository usuarioRepository;
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Around("@annotation(auditAction)")
    public Object around(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
        Object result = joinPoint.proceed();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            return result;
        }

        Usuario actor = usuarioRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(
                null,
                method,
                joinPoint.getArgs(),
                parameterNameDiscoverer
        );
        evaluationContext.setVariable("result", result);

        auditLogService.log(
                auditAction.action(),
                actor == null ? null : actor.getId(),
                authentication.getName(),
                parseUuid(evaluateExpression(auditAction.targetUserIdExpression(), evaluationContext)),
                auditAction.resourceType(),
                evaluateExpression(auditAction.resourceIdExpression(), evaluationContext),
                Map.of()
        );

        return result;
    }

    private String evaluateExpression(String expression, MethodBasedEvaluationContext evaluationContext) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }
        Object value = expressionParser.parseExpression(expression).getValue(evaluationContext);
        return value == null ? null : value.toString();
    }

    private UUID parseUuid(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
