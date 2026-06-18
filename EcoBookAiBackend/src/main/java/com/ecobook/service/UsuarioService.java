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
import com.ecobook.validator.CpfValidator;
import com.ecobook.validation.ProfileCompletionValidation;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
    private final ImageStorageService imageStorageService;

    /**
     * Loads the profile DTO for the user identified by email.
     * @param email authenticated user email
     * @return mapped user profile DTO
     */
    @Cacheable(value = CacheNames.USER_PROFILE, key = "#email", sync = true)
    @Transactional(readOnly = true)
    public UsuarioDTO getByEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        return toDto(usuario);
    }

    /**
     * Updates the authenticated user profile and refreshes completion state.
     * @param email authenticated user email
     * @param request request payload for the operation
     * @return updated result
     */
    @Caching(evict = {
            @CacheEvict(value = CacheNames.USER_PROFILE, key = "#email"),
            @CacheEvict(value = CacheNames.USER_CONSENT_STATUS, key = "#email"),
            @CacheEvict(value = CacheNames.USER_AUTH_CONTEXT, key = "#email")
    })
    @Transactional
    public UsuarioDTO updateProfile(String email, UpdateProfileRequestDTO request) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        validateRequiredFields(request);
        validateWhatsApp(request.getWhatsapp());
        validateCpf(request.getCpf());
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
        usuario.setCpf(CpfValidator.normalize(request.getCpf()));
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

    /**
     * Updates the stored FCM token for the authenticated user.
     * @param email authenticated user email
     * @param token FCM token to store
     */
    @Transactional
    public void updateFcmToken(String email, String token) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        String normalizedToken = token.trim();
        if (normalizedToken.equals(usuario.getFcmToken())) {
            return;
        }

        usuario.setFcmToken(normalizedToken);
        usuarioRepository.save(usuario);
    }

    /**
     * Updates the authenticated user's AI-consent flag.
     * @param email authenticated user email
     * @param consentimentoIa new AI-consent value
     * @return updated result
     */
    @Caching(evict = {
            @CacheEvict(value = CacheNames.USER_PROFILE, key = "#email"),
            @CacheEvict(value = CacheNames.USER_CONSENT_STATUS, key = "#email")
    })
    @Transactional
    public UsuarioDTO updateAiConsent(String email, boolean consentimentoIa) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (Boolean.valueOf(consentimentoIa).equals(usuario.getConsentimentoIa())) {
            return toDto(usuario);
        }

        usuario.setConsentimentoIa(consentimentoIa);
        Usuario savedUser = usuarioRepository.save(usuario);
        consentService.recordAiConsentChange(savedUser, consentimentoIa);
        return toDto(savedUser);
    }

    /**
     * Returns the cached consent summary for the user identified by email.
     * @param email authenticated user email
     * @return current consent summary
     */
    @Cacheable(value = CacheNames.USER_CONSENT_STATUS, key = "#email", sync = true)
    @Transactional(readOnly = true)
    public UserConsentStatusDTO getConsentStatus(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        return consentService.getConsentStatus(usuario);
    }

    /**
     * Maps a user entity into the API DTO representation.
     * @param usuario user entity involved in the operation
     * @return mapped user DTO
     */
    public UsuarioDTO toDto(Usuario usuario) {
        return UsuarioDTO.builder()
                .id(usuario.getId().toString())
                .email(usuario.getEmail())
                .nome(usuario.getNome())
                .whatsapp(usuario.getWhatsapp())
                .cpf(usuario.getCpf())
                .cidade(usuario.getCidade())
                .bairro(usuario.getBairro())
                .instituicao(usuario.getInstituicao())
                .fotoPerfilUrl(UserProfilePhotoPaths.resolveUrl(usuario))
                .perfilCompleto(usuario.isPerfilCompleto())
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
        if (!StringUtils.hasText(request.getCpf())) {
            fieldErrors.put("cpf", "Informe seu CPF");
        }
        if (!StringUtils.hasText(request.getCidade())) {
            fieldErrors.put("cidade", "Informe sua cidade");
        }
        if (!StringUtils.hasText(request.getBairro())) {
            fieldErrors.put("bairro", "Informe seu bairro");
        }

        if (!fieldErrors.isEmpty()) {
            throw new UnprocessableEntityException("Preencha todos os campos obrigatórios do perfil", fieldErrors);
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
                    "O perfil contém campos inválidos",
                    new LinkedHashMap<>(java.util.Map.of("email", "Este email já está cadastrado"))
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
                    "O perfil contém campos inválidos",
                    violations.stream().collect(Collectors.toMap(
                            violation -> violation.getPropertyPath().toString(),
                            ConstraintViolation::getMessage,
                            (left, right) -> left,
                            LinkedHashMap::new
                    ))
            );
        }
    }

    private void validateCpf(String cpf) {
        if (cpf == null) {
            return;
        }

        String normalizedCpf = CpfValidator.normalize(cpf);
        if (!CpfValidator.isValid(normalizedCpf)) {
            throw new BadRequestException(
                    "O perfil contém campos inválidos",
                    new LinkedHashMap<>(Map.of("cpf", "Informe um CPF com 11 dígitos"))
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

        if (fieldErrors.keySet().stream().allMatch(field ->
                field.equals("whatsapp") || field.equals("email") || field.equals("cpf"))) {
            throw new BadRequestException("O perfil contém campos inválidos", fieldErrors);
        }

        throw new UnprocessableEntityException("Preencha todos os campos obrigatórios do perfil", fieldErrors);
    }

    private Set<NecessidadeAcademica> resolveNeeds(Set<NecessidadeAcademica> necessidadesAcademicas) {
        return necessidadesAcademicas == null ? new LinkedHashSet<>() : new LinkedHashSet<>(necessidadesAcademicas);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Caching(evict = {
            @CacheEvict(value = CacheNames.USER_PROFILE, key = "#email"),
            @CacheEvict(value = CacheNames.USER_AUTH_CONTEXT, key = "#email")
    })
    @Transactional
    public UsuarioDTO updateProfilePhoto(String email, MultipartFile image) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        byte[] imageBytes = readImageBytes(image);
        String mimeType = imageStorageService.validateImage(imageBytes);
        Path targetPath = profilePhotoPath(usuario.getId(), mimeType);

        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, imageBytes);
        } catch (IOException exception) {
            throw new ResourceNotFoundException("Não foi possível armazenar a foto de perfil", exception);
        }

        String previousPath = usuario.getFotoPerfilPath();
        usuario.setFotoPerfilPath(targetPath.toString());
        usuario.setFotoPerfilMimeType(mimeType);
        Usuario savedUser = usuarioRepository.save(usuario);

        if (StringUtils.hasText(previousPath) && !previousPath.equals(targetPath.toString())) {
            imageStorageService.deleteIfExists(previousPath);
        }

        return toDto(savedUser);
    }

    @Transactional(readOnly = true)
    public ProfilePhotoPayload loadProfilePhoto(String requesterEmail, String userId) {
        usuarioRepository.findByEmailIgnoreCase(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado"));

        Usuario usuario = usuarioRepository.findById(parseUserId(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (!StringUtils.hasText(usuario.getFotoPerfilPath())) {
            throw new ResourceNotFoundException("Foto de perfil não encontrada");
        }

        Path path = Path.of(usuario.getFotoPerfilPath()).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Foto de perfil não encontrada");
        }

        String contentType = StringUtils.hasText(usuario.getFotoPerfilMimeType())
                ? usuario.getFotoPerfilMimeType().trim()
                : resolvePhotoMimeType(path);
        return new ProfilePhotoPayload(new FileSystemResource(path), contentType);
    }

    public record ProfilePhotoPayload(Resource resource, String contentType) {
    }

    private byte[] readImageBytes(MultipartFile image) {
        try {
            return image.getBytes();
        } catch (IOException exception) {
            throw new BadRequestException("Imagem inválida", Map.of(
                    "image", "Não foi possível ler a foto de perfil enviada. Escolha a imagem novamente e tente de novo."
            ));
        }
    }

    private Path profilePhotoPath(UUID userId, String mimeType) {
        String extension = "image/png".equals(mimeType) ? ".png" : ".jpg";
        return Path.of(imageStorageService.getUploadDir(), userId.toString(), "profile", "profile-photo" + extension)
                .toAbsolutePath()
                .normalize();
    }

    private UUID parseUserId(String rawUserId) {
        if (!StringUtils.hasText(rawUserId)) {
            throw new BadRequestException("Identificador de usuário inválido", Map.of(
                    "user_id", "Informe um UUID válido"
            ));
        }

        try {
            return UUID.fromString(rawUserId.trim());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Identificador de usuário inválido", Map.of(
                    "user_id", "Informe um UUID válido"
            ));
        }
    }

    private String resolvePhotoMimeType(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        return "image/jpeg";
    }
}
