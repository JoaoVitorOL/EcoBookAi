package com.ecobook.service;

import com.ecobook.dto.UpdateProfileRequestDTO;
import com.ecobook.dto.UsuarioDTO;
import com.ecobook.exception.BadRequestException;
import com.ecobook.event.ProfileCompletedEvent;
import com.ecobook.exception.ResourceNotFoundException;
import com.ecobook.exception.UnprocessableEntityException;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.NecessidadeAcademica;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.validation.ProfileCompletionValidation;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User/Usuario service for profile management
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final GeoNormalizationService geoNormalizationService;
    private final Validator validator;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Update user profile with validation
     */
    @Transactional(readOnly = true)
    public UsuarioDTO getByEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
        return toDto(usuario);
    }

    @Transactional
    public UsuarioDTO updateProfile(String email, UpdateProfileRequestDTO request) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        validateRequiredFields(request);
        validateWhatsApp(request.getWhatsapp());

        boolean profileWasComplete = usuario.isPerfilCompleto();
        GeoNormalizationService.NormalizedGeo normalizedGeo =
                geoNormalizationService.normalize(request.getCidade(), request.getBairro());
        validateSupportedCity(normalizedGeo.city());

        usuario.setNome(request.getNome().trim());
        usuario.setWhatsapp(request.getWhatsapp().trim());
        usuario.setCidade(normalizedGeo.city());
        usuario.setBairro(normalizedGeo.neighborhood());
        usuario.setInstituicao(StringUtils.hasText(request.getInstituicao()) ? request.getInstituicao().trim() : null);
        usuario.setConsentimentoIa(Boolean.TRUE.equals(request.getConsentimentoIa()));
        usuario.setNecessidadesAcademicas(resolveNeeds(request.getNecessidadesAcademicas()));
        validateEntityConstraints(usuario);
        usuario.refreshPerfilCompleto();

        Usuario savedUser = usuarioRepository.save(usuario);
        if (!profileWasComplete && savedUser.isPerfilCompleto()) {
            eventPublisher.publishEvent(new ProfileCompletedEvent(savedUser.getId(), savedUser.getEmail()));
        }

        return toDto(savedUser);
    }

    @Transactional
    public void updateFcmToken(String email, String token) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        String normalizedToken = token.trim();
        if (normalizedToken.equals(usuario.getFcmToken())) {
            return;
        }

        usuario.setFcmToken(normalizedToken);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public UsuarioDTO updateAiConsent(String email, boolean consentimentoIa) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        if (Boolean.valueOf(consentimentoIa).equals(usuario.getConsentimentoIa())) {
            return toDto(usuario);
        }

        usuario.setConsentimentoIa(consentimentoIa);
        Usuario savedUser = usuarioRepository.save(usuario);
        return toDto(savedUser);
    }

    public UsuarioDTO toDto(Usuario usuario) {
        return UsuarioDTO.builder()
                .id(usuario.getId().toString())
                .email(usuario.getEmail())
                .nome(usuario.getNome())
                .whatsapp(usuario.getWhatsapp())
                .cidade(usuario.getCidade())
                .bairro(usuario.getBairro())
                .instituicao(usuario.getInstituicao())
                .perfilCompleto(usuario.getPerfilCompleto())
                .consentimentoIa(usuario.getConsentimentoIa())
                .role(usuario.getRole().name())
                .necessidadesAcademicas(usuario.getNecessidadesAcademicas() == null ? Set.of() :
                        usuario.getNecessidadesAcademicas().stream().map(Enum::name).collect(Collectors.toCollection(java.util.LinkedHashSet::new)))
                .criadoEm(usuario.getCriadoEm())
                .atualizadoEm(usuario.getAtualizadoEm())
                .build();
    }

    private void validateRequiredFields(UpdateProfileRequestDTO request) {
        LinkedHashMap<String, String> fieldErrors = new LinkedHashMap<>();

        if (!StringUtils.hasText(request.getNome())) {
            fieldErrors.put("nome", "Informe seu nome");
        }
        if (!StringUtils.hasText(request.getWhatsapp())) {
            fieldErrors.put("whatsapp", "Informe um WhatsApp");
        }
        if (!StringUtils.hasText(request.getCidade())) {
            fieldErrors.put("cidade", "Informe sua cidade");
        }
        if (!StringUtils.hasText(request.getBairro())) {
            fieldErrors.put("bairro", "Informe seu bairro");
        }

        if (!fieldErrors.isEmpty()) {
            throw new UnprocessableEntityException("Preencha todos os campos obrigatorios do perfil", fieldErrors);
        }
    }

    private void validateWhatsApp(String whatsapp) {
        if (whatsapp == null) {
            return;
        }

        Usuario candidate = Usuario.builder()
                .nome("Profile Validation")
                .whatsapp(whatsapp.trim())
                .cidade("CITY")
                .bairro("NEIGHBORHOOD")
                .build();

        Set<ConstraintViolation<Usuario>> violations =
                validator.validateProperty(candidate, "whatsapp", ProfileCompletionValidation.class);

        if (!violations.isEmpty()) {
            throw new BadRequestException(
                    "O perfil contem campos invalidos",
                    violations.stream().collect(Collectors.toMap(
                            violation -> violation.getPropertyPath().toString(),
                            ConstraintViolation::getMessage,
                            (left, right) -> left,
                            LinkedHashMap::new
                    ))
            );
        }
    }

    private void validateEntityConstraints(Usuario usuario) {
        Set<ConstraintViolation<Usuario>> violations = validator.validate(usuario, ProfileCompletionValidation.class);
        LinkedHashMap<String, String> fieldErrors = new LinkedHashMap<>();

        for (ConstraintViolation<Usuario> violation : violations) {
            String field = violation.getPropertyPath().toString();
            if (!fieldErrors.containsKey(field)) {
                fieldErrors.put(field, violation.getMessage());
            }
        }

        if (fieldErrors.isEmpty()) {
            return;
        }

        if (fieldErrors.keySet().stream().allMatch(field -> field.equals("whatsapp"))) {
            throw new BadRequestException("O perfil contem campos invalidos", fieldErrors);
        }

        throw new UnprocessableEntityException("Preencha todos os campos obrigatorios do perfil", fieldErrors);
    }

    private void validateSupportedCity(String normalizedCity) {
        if (geoNormalizationService.isSupportedSouthernCity(normalizedCity)) {
            return;
        }

        throw new UnprocessableEntityException(
                "Preencha todos os campos obrigatorios do perfil",
                Map.of("cidade", "Use uma cidade atendida em SC, PR ou RS.")
        );
    }

    private Set<NecessidadeAcademica> resolveNeeds(Set<NecessidadeAcademica> necessidadesAcademicas) {
        return necessidadesAcademicas == null ? new LinkedHashSet<>() : new LinkedHashSet<>(necessidadesAcademicas);
    }
}
