package com.ecobook.service;

import com.ecobook.config.CacheNames;
import com.ecobook.dto.UpdateProfileRequestDTO;
import com.ecobook.dto.UserConsentStatusDTO;
import com.ecobook.dto.UsuarioDTO;
import com.ecobook.event.ProfileCompletedEvent;
import com.ecobook.exception.BadRequestException;
import com.ecobook.exception.ResourceNotFoundException;
import com.ecobook.exception.UnprocessableEntityException;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.NecessidadeAcademica;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.validation.ProfileCompletionValidation;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User/Usuario service for profile management.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final GeoNormalizationService geoNormalizationService;
    private final Validator validator;
    private final ApplicationEventPublisher eventPublisher;
    private final ConsentService consentService;

    /**
     * Load the authenticated user profile by email.
     */
    @Cacheable(value = CacheNames.USER_PROFILE, key = "#email", sync = true)
    @Transactional(readOnly = true)
    public UsuarioDTO getByEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
        return toDto(usuario);
    }

    @Caching(evict = {
            @CacheEvict(value = CacheNames.USER_PROFILE, key = "#email"),
            @CacheEvict(value = CacheNames.USER_CONSENT_STATUS, key = "#email"),
            @CacheEvict(value = CacheNames.USER_AUTH_CONTEXT, key = "#email")
    })
    @Transactional
    public UsuarioDTO updateProfile(String email, UpdateProfileRequestDTO request) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        validateRequiredFields(request);
        validateWhatsApp(request.getWhatsapp());
        validateEmailChange(usuario, request.getEmail());

        boolean profileWasComplete = usuario.isPerfilCompleto();
        boolean aiConsentWasEnabled = Boolean.TRUE.equals(usuario.getConsentimentoIa());
        GeoNormalizationService.NormalizedGeo normalizedGeo =
                geoNormalizationService.normalize(request.getCidade(), request.getBairro());

        if (StringUtils.hasText(request.getEmail())) {
            usuario.setEmail(normalizeEmail(request.getEmail()));
        }
        usuario.setNome(request.getNome().trim());
        usuario.setWhatsapp(request.getWhatsapp().trim());
        usuario.setCidade(normalizedGeo.city());
        usuario.setBairro(normalizedGeo.neighborhood());
        usuario.setInstituicao(StringUtils.hasText(request.getInstituicao()) ? request.getInstituicao().trim() : null);
        if (request.getConsentimentoIa() != null) {
            usuario.setConsentimentoIa(Boolean.TRUE.equals(request.getConsentimentoIa()));
        }
        if (request.getNecessidadesAcademicas() != null) {
            usuario.setNecessidadesAcademicas(resolveNeeds(request.getNecessidadesAcademicas()));
        }
        validateEntityConstraints(usuario);
        usuario.refreshPerfilCompleto();

        Usuario savedUser = usuarioRepository.save(usuario);
        if (aiConsentWasEnabled != Boolean.TRUE.equals(savedUser.getConsentimentoIa())) {
            consentService.recordAiConsentChange(savedUser, Boolean.TRUE.equals(savedUser.getConsentimentoIa()));
        }
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

    @Caching(evict = {
            @CacheEvict(value = CacheNames.USER_PROFILE, key = "#email"),
            @CacheEvict(value = CacheNames.USER_CONSENT_STATUS, key = "#email")
    })
    @Transactional
    public UsuarioDTO updateAiConsent(String email, boolean consentimentoIa) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        if (Boolean.valueOf(consentimentoIa).equals(usuario.getConsentimentoIa())) {
            return toDto(usuario);
        }

        usuario.setConsentimentoIa(consentimentoIa);
        Usuario savedUser = usuarioRepository.save(usuario);
        consentService.recordAiConsentChange(savedUser, consentimentoIa);
        return toDto(savedUser);
    }

    @Cacheable(value = CacheNames.USER_CONSENT_STATUS, key = "#email", sync = true)
    @Transactional(readOnly = true)
    public UserConsentStatusDTO getConsentStatus(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
        return consentService.getConsentStatus(usuario);
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
                .necessidadesAcademicas(usuario.getNecessidadesAcademicas() == null ? Set.of()
                        : usuario.getNecessidadesAcademicas().stream()
                        .map(Enum::name)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .criadoEm(usuario.getCriadoEm())
                .atualizadoEm(usuario.getAtualizadoEm())
                .build();
    }

    private void validateRequiredFields(UpdateProfileRequestDTO request) {
        LinkedHashMap<String, String> fieldErrors = new LinkedHashMap<>();

        if (request.getEmail() != null && !StringUtils.hasText(request.getEmail())) {
            fieldErrors.put("email", "Informe seu email");
        }
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

    private void validateEmailChange(Usuario usuario, String requestedEmail) {
        if (!StringUtils.hasText(requestedEmail)) {
            return;
        }

        String normalizedEmail = normalizeEmail(requestedEmail);
        if (normalizedEmail.equalsIgnoreCase(usuario.getEmail())) {
            return;
        }

        if (usuarioRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BadRequestException(
                    "O perfil contem campos invalidos",
                    new LinkedHashMap<>(java.util.Map.of("email", "Este email ja esta cadastrado"))
            );
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

        if (fieldErrors.keySet().stream().allMatch(field -> field.equals("whatsapp") || field.equals("email"))) {
            throw new BadRequestException("O perfil contem campos invalidos", fieldErrors);
        }

        throw new UnprocessableEntityException("Preencha todos os campos obrigatorios do perfil", fieldErrors);
    }

    private Set<NecessidadeAcademica> resolveNeeds(Set<NecessidadeAcademica> necessidadesAcademicas) {
        return necessidadesAcademicas == null ? new LinkedHashSet<>() : new LinkedHashSet<>(necessidadesAcademicas);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
