package com.ecobook.aspect;

import com.ecobook.annotation.RequireCompleteProfile;
import com.ecobook.exception.ProfileIncompleteException;
import com.ecobook.model.Usuario;
import com.ecobook.repository.UsuarioRepository;
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

    private final UsuarioRepository usuarioRepository;

    @Before("@annotation(requireCompleteProfile)")
    public void ensureProfileIsComplete(RequireCompleteProfile requireCompleteProfile) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ProfileIncompleteException("Complete your profile before accessing this resource");
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ProfileIncompleteException("Complete your profile before accessing this resource"));

        if (!usuario.isPerfilCompleto()) {
            throw new ProfileIncompleteException("Complete your profile before accessing this resource");
        }
    }
}
