package com.ecobook.service;

import com.ecobook.dto.MaterialDTO;
import com.ecobook.dto.SolicitacaoDTO;
import com.ecobook.dto.UsuarioDTO;
import com.ecobook.exception.ResourceNotFoundException;
import com.ecobook.model.FailedNotification;
import com.ecobook.model.Material;
import com.ecobook.model.UserNotification;
import com.ecobook.model.Usuario;
import com.ecobook.repository.FailedNotificationRepository;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.SolicitacaoRepository;
import com.ecobook.repository.UserNotificationRepository;
import com.ecobook.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class UserDataExportService {

    private final UsuarioRepository usuarioRepository;
    private final MaterialRepository materialRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final FailedNotificationRepository failedNotificationRepository;
    private final UsuarioService usuarioService;
    private final MaterialMapper materialMapper;
    private final SolicitacaoMapper solicitacaoMapper;
    private final ConsentService consentService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ExportedUserData exportCurrentUser(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        UsuarioDTO profile = usuarioService.toDto(usuario);
        List<MaterialDTO> materials = materialRepository.findByDoadorIdOrderByCriadoEmDesc(usuario.getId()).stream()
                .map(materialMapper::toDto)
                .toList();
        List<SolicitacaoDTO> requests = solicitacaoRepository.findByEstudanteIdOrderByCriadoEmDesc(usuario.getId()).stream()
                .map(solicitacaoMapper::toDto)
                .toList();
        List<UserNotification> notifications = userNotificationRepository.findByUserIdOrderByCreatedAtDesc(usuario.getId());
        List<FailedNotification> failedNotifications = failedNotificationRepository.findByUserIdOrderByCreatedAtDesc(usuario.getId());
        List<com.ecobook.dto.ConsentRecordDTO> consentHistory = consentService.getConsentHistory(usuario.getId());
        List<com.ecobook.dto.AuditLogDTO> auditLogs = auditLogService.listUserRelatedLogs(usuario.getId());

        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteStream, StandardCharsets.UTF_8)) {
                writeJson(zipOutputStream, "profile.json", profile);
                writeJson(zipOutputStream, "materials.json", materials);
                writeJson(zipOutputStream, "requests.json", requests);
                writeJson(zipOutputStream, "notifications.json", notifications);
                writeJson(zipOutputStream, "failed-notifications.json", failedNotifications);
                writeJson(zipOutputStream, "consents.json", consentHistory);
                writeJson(zipOutputStream, "audit-log.json", auditLogs);
                writeJson(zipOutputStream, "summary.json", exportSummary(usuario.getId(), materials, requests));
            }

            return new ExportedUserData(
                    "ecobook-dados-" + LocalDate.now() + ".zip",
                    byteStream.toByteArray()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível gerar o arquivo de exportação", exception);
        }
    }

    private Map<String, Object> exportSummary(UUID userId,
                                              List<MaterialDTO> materials,
                                              List<SolicitacaoDTO> requests) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("user_id", userId.toString());
        summary.put("materials_total", materials.size());
        summary.put("requests_total", requests.size());
        summary.put("generated_at", java.time.LocalDateTime.now().toString());
        return summary;
    }

    private void writeJson(ZipOutputStream zipOutputStream, String entryName, Object value) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        zipOutputStream.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(value));
        zipOutputStream.closeEntry();
    }

    public record ExportedUserData(String fileName, byte[] bytes) {
    }
}
