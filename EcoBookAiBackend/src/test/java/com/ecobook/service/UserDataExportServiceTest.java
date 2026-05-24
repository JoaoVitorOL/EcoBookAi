package com.ecobook.service;

import com.ecobook.dto.AuditLogDTO;
import com.ecobook.dto.ConsentRecordDTO;
import com.ecobook.dto.MaterialDTO;
import com.ecobook.dto.SolicitacaoDTO;
import com.ecobook.dto.UsuarioDTO;
import com.ecobook.exception.ResourceNotFoundException;
import com.ecobook.model.FailedNotification;
import com.ecobook.model.Material;
import com.ecobook.model.Solicitacao;
import com.ecobook.model.UserNotification;
import com.ecobook.model.Usuario;
import com.ecobook.repository.FailedNotificationRepository;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.SolicitacaoRepository;
import com.ecobook.repository.UserNotificationRepository;
import com.ecobook.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDataExportServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final MaterialRepository materialRepository = mock(MaterialRepository.class);
    private final SolicitacaoRepository solicitacaoRepository = mock(SolicitacaoRepository.class);
    private final UserNotificationRepository userNotificationRepository = mock(UserNotificationRepository.class);
    private final FailedNotificationRepository failedNotificationRepository = mock(FailedNotificationRepository.class);
    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final MaterialMapper materialMapper = mock(MaterialMapper.class);
    private final SolicitacaoMapper solicitacaoMapper = mock(SolicitacaoMapper.class);
    private final ConsentService consentService = mock(ConsentService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final UserDataExportService userDataExportService = new UserDataExportService(
            usuarioRepository,
            materialRepository,
            solicitacaoRepository,
            userNotificationRepository,
            failedNotificationRepository,
            usuarioService,
            materialMapper,
            solicitacaoMapper,
            consentService,
            auditLogService,
            objectMapper
    );

    @Test
    @DisplayName("exportCurrentUser should package profile, activity and summary files into a zip")
    void shouldExportCurrentUserAsZip() throws Exception {
        UUID userId = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .id(userId)
                .email("export@example.com")
                .nome("Pessoa Exportada")
                .build();
        Material material = mock(Material.class);
        Solicitacao solicitacao = mock(Solicitacao.class);

        when(usuarioRepository.findByEmailIgnoreCase("export@example.com")).thenReturn(Optional.of(usuario));
        when(usuarioService.toDto(usuario)).thenReturn(UsuarioDTO.builder()
                .id(userId.toString())
                .email("export@example.com")
                .nome("Pessoa Exportada")
                .cidade("FLORIANOPOLIS")
                .build());
        when(materialRepository.findByDoadorIdOrderByCriadoEmDesc(userId)).thenReturn(List.of(material));
        when(materialMapper.toDto(material)).thenReturn(MaterialDTO.builder()
                .id(UUID.randomUUID().toString())
                .titulo("Colecao de Quimica")
                .status("DISPONIVEL")
                .build());
        when(solicitacaoRepository.findByEstudanteIdOrderByCriadoEmDesc(userId)).thenReturn(List.of(solicitacao));
        when(solicitacaoMapper.toDto(solicitacao)).thenReturn(SolicitacaoDTO.builder()
                .id(UUID.randomUUID().toString())
                .status("APROVADA")
                .build());
        when(userNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(
                UserNotification.builder()
                        .userId(userId)
                        .notificationId("notif-1")
                        .notificationType("SOLICITACAO_APROVADA")
                        .title("Solicitacao aprovada")
                        .body("Sua solicitacao foi aprovada.")
                        .route("my-requests")
                        .payloadData(Map.of("material_titulo", "Colecao de Quimica"))
                        .build()
        ));
        when(failedNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(
                FailedNotification.builder()
                        .userId(userId)
                        .notificationType("GENERIC")
                        .title("Falha")
                        .body("Entrega pendente")
                        .lastError("firebase indisponivel")
                        .build()
        ));
        when(consentService.getConsentHistory(userId)).thenReturn(List.of(
                ConsentRecordDTO.builder()
                        .id(UUID.randomUUID().toString())
                        .consentType("PLATFORM")
                        .status("GIVEN")
                        .createdAt(LocalDateTime.now().minusDays(1))
                        .build()
        ));
        when(auditLogService.listUserRelatedLogs(userId)).thenReturn(List.of(
                AuditLogDTO.builder()
                        .id(UUID.randomUUID().toString())
                        .action("USER_EXPORT")
                        .createdAt(LocalDateTime.now())
                        .build()
        ));

        UserDataExportService.ExportedUserData exported = userDataExportService.exportCurrentUser("export@example.com");
        Map<String, String> zipEntries = unzipEntries(exported.bytes());

        assertThat(exported.fileName()).isEqualTo("ecobook-dados-" + LocalDate.now() + ".zip");
        assertThat(zipEntries).containsKeys(
                "profile.json",
                "materials.json",
                "requests.json",
                "notifications.json",
                "failed-notifications.json",
                "consents.json",
                "audit-log.json",
                "summary.json"
        );

        JsonNode profile = objectMapper.readTree(zipEntries.get("profile.json"));
        JsonNode summary = objectMapper.readTree(zipEntries.get("summary.json"));
        JsonNode materials = objectMapper.readTree(zipEntries.get("materials.json"));

        assertThat(profile.path("email").asText()).isEqualTo("export@example.com");
        assertThat(materials.get(0).path("titulo").asText()).isEqualTo("Colecao de Quimica");
        assertThat(summary.path("user_id").asText()).isEqualTo(userId.toString());
        assertThat(summary.path("materials_total").asInt()).isEqualTo(1);
        assertThat(summary.path("requests_total").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("exportCurrentUser should fail when the authenticated user no longer exists")
    void shouldFailWhenUserDoesNotExist() {
        when(usuarioRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDataExportService.exportCurrentUser("missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Map<String, String> unzipEntries(byte[] bytes) throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8));
                zipInputStream.closeEntry();
            }
        }
        return entries;
    }
}
