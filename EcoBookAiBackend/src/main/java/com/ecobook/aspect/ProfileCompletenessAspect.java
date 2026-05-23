package com.ecobook.aspect;

import com.ecobook.annotation.RequireCompleteProfile;
import com.ecobook.exception.ProfileIncompleteException;
import com.ecobook.security.AuthenticatedUserLookupService;
import com.ecobook.security.AuthenticatedUserSnapshot;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ProfileCompletenessAspect {

    private final AuthenticatedUserLookupService authenticatedUserLookupService;

    @Before("@annotation(requireCompleteProfile)")
    public void ensureProfileIsComplete(RequireCompleteProfile requireCompleteProfile) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ProfileIncompleteException("Conclua seu perfil antes de acessar este recurso");
        }

        AuthenticatedUserSnapshot usuario;
        try {
            usuario = authenticatedUserLookupService.loadRequiredByEmail(authentication.getName());
        } catch (RuntimeException ex) {
            throw new ProfileIncompleteException("Conclua seu perfil antes de acessar este recurso");
        }

        if (!usuario.profileComplete()) {
            throw new ProfileIncompleteException("Conclua seu perfil antes de acessar este recurso");
        }
    }
}
