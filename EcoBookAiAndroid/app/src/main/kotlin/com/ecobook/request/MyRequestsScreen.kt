package com.ecobook.request

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecobook.dto.SolicitacaoDTO
import com.ecobook.ui.WhatsAppFormatter
import com.ecobook.ui.components.FilterChipCard
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.SectionHeading

@Composable
fun MyRequestsScreen(
    unreadNotifications: Int = 0,
    onOpenNotifications: () -> Unit = {},
    viewModel: MyRequestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingCancellation by remember { mutableStateOf<SolicitacaoDTO?>(null) }
    var pendingReport by remember { mutableStateOf<SolicitacaoDTO?>(null) }
    var reportReason by remember { mutableStateOf("") }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SectionHeading(
                title = "Minhas solicitações",
                subtitle = "Acompanhe os pedidos que você abriu, veja quando o contato for liberado e cancele quando precisar.",
                trailingContent = {
                    com.ecobook.ui.components.NotificationsEntryPointButton(
                        unreadCount = unreadNotifications,
                        onClick = onOpenNotifications
                    )
                }
            )
        }

        item {
            GlassCard {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MyRequestFilter.entries.forEach { filter ->
                        FilterChipCard(
                            label = filter.label,
                            selected = uiState.selectedFilter == filter,
                            onClick = { viewModel.updateFilter(filter) }
                        )
                    }
                }
            }
        }

        uiState.errorMessage?.let { message ->
            item {
                GlassCard {
                    Text(
                        text = "Não foi possível carregar suas solicitações.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (uiState.isLoading && uiState.requests.isEmpty()) {
            item {
                GlassCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text(
                            text = "Carregando suas solicitações...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (uiState.visibleRequests.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = if (uiState.requests.isEmpty()) {
                            "Você ainda não solicitou nenhum material."
                        } else {
                            "Nenhuma solicitação encontrada nesse filtro."
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Quando você pedir um material pela busca, o acompanhamento passa a aparecer aqui.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.visibleRequests, key = { it.id }) { request ->
                StudentRequestCard(
                    request = request,
                    isWorking = uiState.activeRequestId == request.id,
                    onCancel = { pendingCancellation = request },
                    onContactDonor = { openWhatsApp(context, request) },
                    onReportNonReceipt = if (request.status == "CONCLUIDA") {
                        {
                            pendingReport = request
                            reportReason = ""
                        }
                    } else {
                        null
                    },
                    hasReportedNonReceipt = request.id in uiState.reportedRequestIds
                )
            }
        }
    }

    pendingCancellation?.let { request ->
        AlertDialog(
            onDismissRequest = { pendingCancellation = null },
            title = { Text("Cancelar solicitação") },
            text = {
                Text(
                    "Você vai cancelar a solicitação de \"${request.material?.titulo ?: "este material"}\". Depois disso, será preciso fazer um novo pedido se ainda quiser receber esse material."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingCancellation = null
                        viewModel.cancelRequest(request.id)
                    },
                    enabled = uiState.activeRequestId == null
                ) {
                    Text(if (uiState.activeRequestId == request.id) "Cancelando..." else "Cancelar solicitação")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingCancellation = null }) {
                    Text("Manter solicitação")
                }
            }
        )
    }

    pendingReport?.let { request ->
        AlertDialog(
            onDismissRequest = {
                pendingReport = null
                reportReason = ""
            },
            title = { Text("Reportar não recebimento") },
            text = {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Se você concluiu a solicitação, mas o material não chegou, conte o que aconteceu. O motivo é opcional."
                    )
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it.take(500) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        label = { Text("Motivo (opcional)") },
                        supportingText = { Text("${reportReason.length}/500") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val selectedRequest = pendingReport
                        val currentReason = reportReason
                        pendingReport = null
                        reportReason = ""
                        if (selectedRequest != null) {
                            viewModel.reportNonReceipt(selectedRequest, currentReason)
                        }
                    },
                    enabled = uiState.activeRequestId == null
                ) {
                    Text(if (uiState.activeRequestId == request.id) "Enviando..." else "Enviar reporte")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        pendingReport = null
                        reportReason = ""
                    }
                ) {
                    Text("Fechar")
                }
            }
        )
    }
}

private fun openWhatsApp(context: android.content.Context, request: SolicitacaoDTO) {
    val donorWhatsapp = request.contatoDoador?.get("whatsapp").orEmpty()
    val normalized = WhatsAppFormatter.format(donorWhatsapp).filter(Char::isDigit)
    if (normalized.isBlank()) {
        Toast.makeText(context, "O contato do doador ainda não está disponível.", Toast.LENGTH_SHORT).show()
        return
    }

    val title = request.material?.titulo ?: "material solicitado"
    val message = Uri.encode("Oi! Estou entrando em contato sobre o material \"$title\" no seu aplicativo EcoBook.")
    val nativeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?phone=$normalized&text=$message"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$normalized?text=$message"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        context.startActivity(nativeIntent)
        return
    } catch (_: ActivityNotFoundException) {
        // Fall back to the public web URL when the native app is not installed.
    }

    try {
        context.startActivity(webIntent)
        return
    } catch (_: ActivityNotFoundException) {
        copyContactToClipboard(context, donorWhatsapp.ifBlank { normalized })
        Toast.makeText(
            context,
            "Nenhum app compatível foi encontrado. O contato do doador foi copiado.",
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun copyContactToClipboard(context: Context, contact: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Contato do doador", contact))
}
