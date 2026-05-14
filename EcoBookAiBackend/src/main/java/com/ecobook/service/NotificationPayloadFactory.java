package com.ecobook.service;

import com.ecobook.dto.notification.NotificationPayloadDTO;
import com.ecobook.dto.notification.NotificationType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class NotificationPayloadFactory {

    public NotificationPayloadDTO requestReceived(String requestId,
                                                  String materialId,
                                                  String materialTitle,
                                                  String studentName) {
        return build(
                NotificationType.SOLICITACAO_RECEBIDA,
                "Novo pedido recebido",
                "Sua doacao \"" + materialTitle + "\" recebeu uma nova solicitacao.",
                requestId,
                materialId,
                Map.ofEntries(
                        Map.entry("material_titulo", materialTitle),
                        Map.entry("estudante_nome", studentName)
                )
        );
    }

    public NotificationPayloadDTO requestApproved(String requestId,
                                                  String materialId,
                                                  String materialTitle,
                                                  String donorName,
                                                  String donorWhatsapp) {
        return build(
                NotificationType.SOLICITACAO_APROVADA,
                "Solicitacao aprovada",
                "Sua solicitacao para \"" + materialTitle + "\" foi aprovada.",
                requestId,
                materialId,
                metadata(
                        "material_titulo", materialTitle,
                        "doador_nome", donorName,
                        "doador_whatsapp", donorWhatsapp
                )
        );
    }

    public NotificationPayloadDTO requestDeclined(String requestId,
                                                  String materialId,
                                                  String materialTitle) {
        return build(
                NotificationType.SOLICITACAO_RECUSADA,
                "Solicitacao recusada",
                "O pedido para \"" + materialTitle + "\" foi recusado.",
                requestId,
                materialId,
                Map.of("material_titulo", materialTitle)
        );
    }

    public NotificationPayloadDTO requestCanceled(String requestId,
                                                  String materialId,
                                                  String materialTitle,
                                                  String messageBody) {
        return build(
                NotificationType.SOLICITACAO_CANCELADA,
                "Solicitacao cancelada",
                messageBody,
                requestId,
                materialId,
                Map.of("material_titulo", materialTitle)
        );
    }

    public NotificationPayloadDTO requestExpired(String requestId,
                                                 String materialId,
                                                 String materialTitle) {
        return build(
                NotificationType.SOLICITACAO_CANCELADA,
                "Reserva expirada",
                "A reserva do material \"" + materialTitle + "\" expirou e o item voltou a ficar disponivel.",
                requestId,
                materialId,
                Map.of("material_titulo", materialTitle)
        );
    }

    public NotificationPayloadDTO donationCompleted(String requestId,
                                                    String materialId,
                                                    String materialTitle,
                                                    String donorName,
                                                    String donorWhatsapp) {
        return build(
                NotificationType.MATERIAL_DOADO,
                "Doacao concluida",
                "O material \"" + materialTitle + "\" foi marcado como doado.",
                requestId,
                materialId,
                metadata(
                        "material_titulo", materialTitle,
                        "doador_nome", donorName,
                        "doador_whatsapp", donorWhatsapp
                )
        );
    }

    public NotificationPayloadDTO materialCanceled(String requestId,
                                                   String materialId,
                                                   String materialTitle) {
        return build(
                NotificationType.MATERIAL_CANCELADO,
                "Material removido",
                "A doacao \"" + materialTitle + "\" foi removida pelo doador.",
                requestId,
                materialId,
                Map.of("material_titulo", materialTitle)
        );
    }

    private NotificationPayloadDTO build(NotificationType type,
                                         String title,
                                         String body,
                                         String requestId,
                                         String materialId,
                                         Map<String, String> metadata) {
        return NotificationPayloadDTO.builder()
                .notificationId(UUID.randomUUID().toString())
                .type(type)
                .title(title)
                .body(body)
                .route(type.getRoute())
                .requestId(requestId)
                .materialId(materialId)
                .metadata(metadata)
                .build();
    }

    private Map<String, String> metadata(String... pairs) {
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        for (int index = 0; index + 1 < pairs.length; index += 2) {
            String key = pairs[index];
            String value = pairs[index + 1];
            if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                metadata.put(key, value.trim());
            }
        }
        return metadata;
    }
}
