package com.ecobook.service;

import com.ecobook.dto.notification.NotificationPayloadDTO;
import com.ecobook.dto.notification.NotificationType;
import com.ecobook.model.Usuario;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class NotificationPayloadFactory {

    /**
     * Builds the payload for the request received notification.
     *
     * @param requestId the requestId value
     * @param materialId the material identifier
     * @param materialTitle the materialTitle value
     * @param solicitante the solicitante value
     * @return the notification payload
     */
    public NotificationPayloadDTO requestReceived(String requestId,
                                                  String materialId,
                                                  String materialTitle,
                                                  Usuario solicitante) {
        return build(
                NotificationType.SOLICITACAO_RECEBIDA,
                "Novo pedido recebido",
                safeName(solicitante, "Um solicitante") +
                        " pediu o material \"" + materialTitle + "\". Revise os dados do solicitante antes de aprovar ou recusar.",
                requestId,
                materialId,
                participantMetadata(materialTitle, null, solicitante)
        );
    }

    /**
     * Builds the payload for the request approved notification.
     *
     * @param requestId the requestId value
     * @param materialId the material identifier
     * @param materialTitle the materialTitle value
     * @param doador the doador value
     * @param solicitante the solicitante value
     * @return the notification payload
     */
    public NotificationPayloadDTO requestApproved(String requestId,
                                                  String materialId,
                                                  String materialTitle,
                                                  Usuario doador,
                                                  Usuario solicitante) {
        return build(
                NotificationType.SOLICITACAO_APROVADA,
                "Solicitacao aprovada",
                "Sua solicitacao do material \"" + materialTitle + "\" foi aprovada por " +
                        safeName(doador, "o doador") +
                        ". O contato do doador ja esta disponivel no pedido.",
                requestId,
                materialId,
                participantMetadata(materialTitle, doador, solicitante)
        );
    }

    /**
     * Builds the payload for the request declined notification.
     *
     * @param requestId the requestId value
     * @param materialId the material identifier
     * @param materialTitle the materialTitle value
     * @param doador the doador value
     * @param solicitante the solicitante value
     * @return the notification payload
     */
    public NotificationPayloadDTO requestDeclined(String requestId,
                                                  String materialId,
                                                  String materialTitle,
                                                  Usuario doador,
                                                  Usuario solicitante) {
        return build(
                NotificationType.SOLICITACAO_RECUSADA,
                "Solicitacao recusada",
                "Sua solicitacao do material \"" + materialTitle + "\" foi recusada por " +
                        safeName(doador, "o doador") + ".",
                requestId,
                materialId,
                participantMetadata(materialTitle, doador, solicitante)
        );
    }

    /**
     * Builds the payload for the request canceled by donor notification.
     *
     * @param requestId the requestId value
     * @param materialId the material identifier
     * @param materialTitle the materialTitle value
     * @param doador the doador value
     * @param solicitante the solicitante value
     * @return the notification payload
     */
    public NotificationPayloadDTO requestCanceledByDonor(String requestId,
                                                         String materialId,
                                                         String materialTitle,
                                                         Usuario doador,
                                                         Usuario solicitante) {
        LinkedHashMap<String, String> metadata = participantMetadata(materialTitle, doador, solicitante);
        metadata.put("cancelado_por", "DOADOR");
        return build(
                NotificationType.SOLICITACAO_CANCELADA,
                "Solicitacao cancelada pelo doador",
                "Sua solicitacao do material \"" + materialTitle + "\" foi cancelada pelo doador " +
                        safeName(doador, "responsavel pelo material") + ".",
                requestId,
                materialId,
                metadata
        );
    }

    /**
     * Builds the payload for the request canceled by student notification.
     *
     * @param requestId the requestId value
     * @param materialId the material identifier
     * @param materialTitle the materialTitle value
     * @param doador the doador value
     * @param solicitante the solicitante value
     * @return the notification payload
     */
    public NotificationPayloadDTO requestCanceledByStudent(String requestId,
                                                           String materialId,
                                                           String materialTitle,
                                                           Usuario doador,
                                                           Usuario solicitante) {
        LinkedHashMap<String, String> metadata = participantMetadata(materialTitle, doador, solicitante);
        metadata.put("cancelado_por", "SOLICITANTE");
        return build(
                NotificationType.SOLICITACAO_CANCELADA,
                "Solicitacao cancelada pelo solicitante",
                "O solicitante " + safeName(solicitante, "responsavel pelo pedido") +
                        " cancelou o pedido do material \"" + materialTitle + "\".",
                requestId,
                materialId,
                metadata
        );
    }

    /**
     * Builds the payload for the request expired notification.
     *
     * @param requestId the requestId value
     * @param materialId the material identifier
     * @param materialTitle the materialTitle value
     * @param doador the doador value
     * @param solicitante the solicitante value
     * @return the notification payload
     */
    public NotificationPayloadDTO requestExpired(String requestId,
                                                 String materialId,
                                                 String materialTitle,
                                                 Usuario doador,
                                                 Usuario solicitante) {
        LinkedHashMap<String, String> metadata = participantMetadata(materialTitle, doador, solicitante);
        metadata.put("cancelado_por", "PRAZO");
        return build(
                NotificationType.SOLICITACAO_CANCELADA,
                "Reserva expirada",
                "A reserva do material \"" + materialTitle + "\" expirou porque a doacao nao foi concluida dentro do prazo.",
                requestId,
                materialId,
                metadata
        );
    }

    /**
     * Builds the payload for the donation completed notification.
     *
     * @param requestId the requestId value
     * @param materialId the material identifier
     * @param materialTitle the materialTitle value
     * @param doador the doador value
     * @param solicitante the solicitante value
     * @return the notification payload
     */
    public NotificationPayloadDTO donationCompleted(String requestId,
                                                    String materialId,
                                                    String materialTitle,
                                                    Usuario doador,
                                                    Usuario solicitante) {
        return build(
                NotificationType.MATERIAL_DOADO,
                "Doacao concluida",
                "A doacao do material \"" + materialTitle + "\" foi concluida entre " +
                        safeName(doador, "o doador") +
                        " e " + safeName(solicitante, "o solicitante") + ".",
                requestId,
                materialId,
                participantMetadata(materialTitle, doador, solicitante)
        );
    }

    /**
     * Builds the payload for the material canceled notification.
     *
     * @param requestId the requestId value
     * @param materialId the material identifier
     * @param materialTitle the materialTitle value
     * @param doador the doador value
     * @param solicitante the solicitante value
     * @return the notification payload
     */
    public NotificationPayloadDTO materialCanceled(String requestId,
                                                   String materialId,
                                                   String materialTitle,
                                                   Usuario doador,
                                                   Usuario solicitante) {
        return build(
                NotificationType.MATERIAL_CANCELADO,
                "Material removido",
                "O doador " + safeName(doador, "responsavel pelo material") +
                        " removeu o material \"" + materialTitle + "\" e a solicitacao associada foi encerrada.",
                requestId,
                materialId,
                participantMetadata(materialTitle, doador, solicitante)
        );
    }

    /**
     * Builds the payload for the material removed by admin notification.
     *
     * @param requestId the requestId value
     * @param materialId the material identifier
     * @param materialTitle the materialTitle value
     * @param doador the doador value
     * @param solicitante the solicitante value
     * @return the notification payload
     */
    public NotificationPayloadDTO materialRemovedByAdmin(String requestId,
                                                         String materialId,
                                                         String materialTitle,
                                                         Usuario doador,
                                                         Usuario solicitante) {
        return build(
                NotificationType.MATERIAL_CANCELADO,
                "Material removido pela equipe",
                "A equipe EcoBook removeu o material \"" + materialTitle +
                        "\" da plataforma durante uma revisao administrativa.",
                requestId,
                materialId,
                participantMetadata(materialTitle, doador, solicitante)
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

    private LinkedHashMap<String, String> participantMetadata(String materialTitle,
                                                              Usuario doador,
                                                              Usuario solicitante) {
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        putIfHasText(metadata, "material_titulo", materialTitle);
        addParticipant(metadata, "doador", doador, true);
        addParticipant(metadata, "solicitante", solicitante, false);
        return metadata;
    }

    private void addParticipant(Map<String, String> target,
                                String prefix,
                                Usuario usuario,
                                boolean includeWhatsapp) {
        if (usuario == null) {
            return;
        }

        putIfHasText(target, prefix + "_nome", usuario.getNome());
        putIfHasText(target, prefix + "_cidade", usuario.getCidade());
        putIfHasText(target, prefix + "_bairro", usuario.getBairro());
        putIfHasText(target, prefix + "_instituicao", usuario.getInstituicao());
        if (includeWhatsapp) {
            putIfHasText(target, prefix + "_whatsapp", usuario.getWhatsapp());
        }
    }

    private String safeName(Usuario usuario, String fallback) {
        if (usuario == null || !StringUtils.hasText(usuario.getNome())) {
            return fallback;
        }
        return usuario.getNome().trim();
    }

    private void putIfHasText(Map<String, String> target, String key, String value) {
        if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
            target.put(key, value.trim());
        }
    }
}
