package com.ecobook.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Drafts
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecobook.fcm.NotificationInboxEntry
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.StatusBadge
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NotificationsScreen(
    topPadding: PaddingValues = PaddingValues(),
    onOpenNotification: (NotificationInboxEntry) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.onScreenOpened()
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LazyColumn(
        modifier = Modifier.padding(topPadding),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 20.dp,
            bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            GlassCard {
                Text(
                    text = if (uiState.unreadCount > 0) {
                        "Voce tem ${uiState.unreadCount} notificacoes pendentes."
                    } else {
                        "Central de notificacoes"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (uiState.unreadCount > 0) {
                        "Aqui ficam apenas avisos ainda nao lidos. Assim que voce marcar uma notificacao como lida, ela sai da lista."
                    } else {
                        "A central sincroniza automaticamente ao abrir esta tela e ao voltar para ela. So aparecem avisos ainda pendentes de leitura."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when {
                    uiState.isRefreshing -> StatusBadge(
                        text = "Sincronizando automaticamente",
                        containerColor = Color(0xFFFCE7D8),
                        contentColor = Color(0xFF8A4C1F)
                    )

                    uiState.unreadCount > 0 -> StatusBadge(
                        text = "Pendentes de leitura",
                        containerColor = Color(0xFFE0EFE4),
                        contentColor = Color(0xFF205447)
                    )

                    else -> StatusBadge(
                        text = "Tudo em dia",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.unreadCount > 0) {
                    Button(
                        onClick = viewModel::markAllAsRead,
                        enabled = !uiState.isMarkingAllAsRead && uiState.activeReadNotificationId == null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (uiState.isMarkingAllAsRead) {
                                "Limpando a central..."
                            } else {
                                "Marcar todas como lidas"
                            }
                        )
                    }
                }
            }
        }

        val errorMessage = uiState.errorMessage

        if (errorMessage != null && uiState.notifications.isNotEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = "A central registrou uma falha na ultima sincronizacao.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Quando esta tela voltar ao foco, a sincronizacao sera tentada novamente automaticamente.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (uiState.notifications.isEmpty() && errorMessage == null) {
            item {
                GlassCard {
                    Text(
                        text = "Nenhuma notificacao pendente no momento.",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Quando houver movimentacao nas solicitacoes e doacoes, os avisos novos aparecem aqui ate voce marca-los como lidos.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (uiState.notifications.isEmpty() && errorMessage != null) {
            item {
                GlassCard {
                    Text(
                        text = "A central ainda nao conseguiu buscar novas notificacoes.",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Quando esta tela for reaberta ou voltar ao foco, uma nova sincronizacao sera tentada automaticamente.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.notifications, key = { it.id }) { notification ->
                NotificationCard(
                    notification = notification,
                    isMarkingAsRead = uiState.activeReadNotificationId == notification.id,
                    onOpen = { onOpenNotification(notification) },
                    onMarkAsRead = { viewModel.markAsRead(notification.id) }
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationInboxEntry,
    isMarkingAsRead: Boolean,
    onOpen: () -> Unit,
    onMarkAsRead: () -> Unit
) {
    val style = notificationVisualStyle(notification)
    val contextLines = notificationContextLines(notification)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(style.containerColor, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = style.contentColor
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = notification.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(
                    text = "Nova",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (contextLines.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    contextLines.forEach { (label, value) ->
                        Text(
                            text = "$label: $value",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = formatTimestamp(notification.receivedAtEpochMillis),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Abrir")
                }
                TextButton(
                    onClick = onMarkAsRead,
                    enabled = !isMarkingAsRead,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isMarkingAsRead) "Marcando..." else "Marcar como lida")
                }
            }
        }
    }
}

private fun notificationContextLines(notification: NotificationInboxEntry): List<Pair<String, String>> {
    val metadata = notification.metadata
    val lines = mutableListOf<Pair<String, String>>()

    metadata["material_titulo"]?.let { lines += "Material" to it }
    metadata["doador_nome"]?.let { lines += "Doador" to it }
    formatLocation(metadata["doador_bairro"], metadata["doador_cidade"])
        ?.let { lines += "Local do doador" to it }
    metadata["doador_instituicao"]?.let { lines += "Instituicao do doador" to it }
    metadata["solicitante_nome"]?.let { lines += "Solicitante" to it }
    formatLocation(metadata["solicitante_bairro"], metadata["solicitante_cidade"])
        ?.let { lines += "Local do solicitante" to it }
    metadata["solicitante_instituicao"]?.let { lines += "Instituicao do solicitante" to it }

    return lines
}

private fun formatLocation(bairro: String?, cidade: String?): String? {
    val normalizedBairro = bairro?.trim().orEmpty()
    val normalizedCidade = cidade?.trim().orEmpty()

    return when {
        normalizedBairro.isNotBlank() && normalizedCidade.isNotBlank() -> "$normalizedBairro, $normalizedCidade"
        normalizedCidade.isNotBlank() -> normalizedCidade
        normalizedBairro.isNotBlank() -> normalizedBairro
        else -> null
    }
}

private fun notificationVisualStyle(notification: NotificationInboxEntry): NotificationVisualStyle {
    val notificationType = notification.notificationType
    val canceladoPor = notification.metadata["cancelado_por"]?.trim()?.uppercase()
    val semanticHint = notificationSemanticHint(notification)

    return when {
        notificationType == "SOLICITACAO_RECEBIDA" || semanticHint == NotificationSemantic.RECEIVED -> NotificationVisualStyle(
            icon = Icons.Rounded.Drafts,
            containerColor = Color(0xFFFCE7D8),
            contentColor = Color(0xFF8A4C1F)
        )

        notificationType == "SOLICITACAO_APROVADA" || semanticHint == NotificationSemantic.APPROVED -> NotificationVisualStyle(
            icon = Icons.Rounded.CheckCircle,
            containerColor = Color(0xFFE0EFE4),
            contentColor = Color(0xFF205447)
        )

        notificationType == "MATERIAL_DOADO" || semanticHint == NotificationSemantic.DONATED -> NotificationVisualStyle(
            icon = Icons.Rounded.VolunteerActivism,
            containerColor = Color(0xFFE3EEF8),
            contentColor = Color(0xFF234C73)
        )

        notificationType == "SOLICITACAO_RECUSADA" || semanticHint == NotificationSemantic.DECLINED -> NotificationVisualStyle(
            icon = Icons.Rounded.ThumbDown,
            containerColor = Color(0xFFFBE4DF),
            contentColor = Color(0xFF8B4032)
        )

        notificationType == "SOLICITACAO_CANCELADA" && canceladoPor == "DOADOR" -> NotificationVisualStyle(
            icon = Icons.Rounded.PersonRemove,
            containerColor = Color(0xFFF7DDDB),
            contentColor = Color(0xFF8D3D30)
        )

        notificationType == "SOLICITACAO_CANCELADA" && canceladoPor == "SOLICITANTE" -> NotificationVisualStyle(
            icon = Icons.Rounded.Cancel,
            containerColor = Color(0xFFF8E6E1),
            contentColor = Color(0xFF7E3E39)
        )

        notificationType == "SOLICITACAO_CANCELADA" && canceladoPor == "PRAZO" -> NotificationVisualStyle(
            icon = Icons.Rounded.Schedule,
            containerColor = Color(0xFFFDECCF),
            contentColor = Color(0xFF815B14)
        )

        notificationType == "MATERIAL_CANCELADO" || semanticHint == NotificationSemantic.REMOVED -> NotificationVisualStyle(
            icon = Icons.Rounded.Close,
            containerColor = Color(0xFFF7DDDB),
            contentColor = Color(0xFF8D3D30)
        )

        notificationType == "SOLICITACAO_CANCELADA" || semanticHint == NotificationSemantic.CANCELED -> NotificationVisualStyle(
            icon = Icons.Rounded.Cancel,
            containerColor = Color(0xFFF7DDDB),
            contentColor = Color(0xFF8D3D30)
        )

        else -> NotificationVisualStyle(
            icon = Icons.Rounded.Notifications,
            containerColor = Color(0xFFF0F1F3),
            contentColor = Color(0xFF4B5563)
        )
    }
}

private fun notificationSemanticHint(notification: NotificationInboxEntry): NotificationSemantic {
    val haystack = buildString {
        append(notification.title)
        append(' ')
        append(notification.body)
    }.lowercase()

    return when {
        "aprovad" in haystack -> NotificationSemantic.APPROVED
        "novo pedido" in haystack || "pedido recebido" in haystack || "solicitou o material" in haystack -> NotificationSemantic.RECEIVED
        "recusad" in haystack -> NotificationSemantic.DECLINED
        "removid" in haystack || "removeu o material" in haystack || "exclu" in haystack -> NotificationSemantic.REMOVED
        "expir" in haystack -> NotificationSemantic.EXPIRED
        "cancelad" in haystack -> NotificationSemantic.CANCELED
        "doacao concluida" in haystack || "foi concluida" in haystack -> NotificationSemantic.DONATED
        else -> NotificationSemantic.GENERIC
    }
}

private fun formatTimestamp(receivedAtEpochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    return Instant.ofEpochMilli(receivedAtEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(formatter)
}

private data class NotificationVisualStyle(
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color
)

private enum class NotificationSemantic {
    RECEIVED,
    APPROVED,
    DECLINED,
    CANCELED,
    REMOVED,
    DONATED,
    EXPIRED,
    GENERIC
}
