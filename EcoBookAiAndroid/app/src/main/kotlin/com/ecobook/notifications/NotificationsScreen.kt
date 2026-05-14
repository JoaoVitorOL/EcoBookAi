package com.ecobook.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
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
                        "Voce tem ${uiState.unreadCount} notificacoes novas para revisar."
                    } else {
                        "Central de notificacoes"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (uiState.unreadCount > 0) {
                        "Revise avisos recentes sobre pedidos, aprovacoes, recusas e entregas. A leitura agora e controlada manualmente por item ou em lote."
                    } else {
                        "Use esta tela para revisar avisos de pedidos, aprovacoes, recusas e entregas sempre que houver movimentacao."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = viewModel::refresh,
                        enabled = !uiState.isRefreshing && !uiState.isMarkingAllAsRead,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.isRefreshing) "Atualizando..." else "Atualizar")
                    }
                    if (uiState.unreadCount > 0) {
                        Button(
                            onClick = viewModel::markAllAsRead,
                            enabled = !uiState.isMarkingAllAsRead &&
                                uiState.activeReadNotificationId == null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (uiState.isMarkingAllAsRead) {
                                    "Marcando todas como lidas..."
                                } else {
                                    "Marcar todas como lidas"
                                }
                            )
                        }
                    } else {
                        StatusBadge(
                            text = "Tudo em dia",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                        text = "Nao foi possivel sincronizar a central de notificacoes.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = errorMessage,
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
                        text = "Nenhuma notificacao por enquanto.",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Quando houver movimentacao nas solicitacoes e doacoes, o historico passa a aparecer aqui.",
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
                        text = "Assim que a conexao com o backend voltar, toque em Atualizar para carregar o historico mais recente.",
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
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (notification.unread) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                StatusBadge(
                    text = if (notification.unread) "Nova" else "Lida",
                    containerColor = if (notification.unread) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (notification.unread) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Text(
                text = notification.body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                if (notification.unread) {
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
}

private fun formatTimestamp(receivedAtEpochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    return Instant.ofEpochMilli(receivedAtEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(formatter)
}
