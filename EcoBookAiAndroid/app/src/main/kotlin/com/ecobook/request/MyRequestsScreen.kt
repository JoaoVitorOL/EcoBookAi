package com.ecobook.request

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecobook.dto.SolicitacaoDTO
import com.ecobook.ui.WhatsAppFormatter
import com.ecobook.ui.components.FilterChipCard
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.SectionHeading

@Composable
fun MyRequestsScreen(
    viewModel: MyRequestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SectionHeading(
                title = "Minhas solicitacoes",
                subtitle = "Acompanhe os pedidos que voce abriu, veja quando o contato for liberado e cancele quando precisar."
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
                OutlinedButton(onClick = viewModel::refresh, enabled = !uiState.isLoading) {
                    Text("Atualizar lista")
                }
            }
        }

        uiState.errorMessage?.let { message ->
            item {
                GlassCard {
                    Text(
                        text = "Nao foi possivel carregar suas solicitacoes.",
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
                            text = "Carregando suas solicitacoes...",
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
                            "Voce ainda nao solicitou nenhum material."
                        } else {
                            "Nenhuma solicitacao encontrada nesse filtro."
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Quando voce pedir um material pela busca, o acompanhamento passa a aparecer aqui.",
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
                    onCancel = { viewModel.cancelRequest(request.id) },
                    onContactDonor = { openWhatsApp(context, request) }
                )
            }
        }
    }
}

private fun openWhatsApp(context: android.content.Context, request: SolicitacaoDTO) {
    val donorWhatsapp = request.contatoDoador?.get("whatsapp").orEmpty()
    val normalized = WhatsAppFormatter.format(donorWhatsapp).filter(Char::isDigit)
    if (normalized.isBlank()) {
        Toast.makeText(context, "O contato do doador ainda nao esta disponivel.", Toast.LENGTH_SHORT).show()
        return
    }

    val title = request.material?.titulo ?: "material solicitado"
    val message = Uri.encode("Oi! Estou entrando em contato sobre o material \"$title\".")
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$normalized?text=$message"))

    if (intent.resolveActivity(context.packageManager) == null) {
        Toast.makeText(context, "Nao foi encontrado um app compativel com WhatsApp neste dispositivo.", Toast.LENGTH_SHORT).show()
        return
    }

    context.startActivity(intent)
}
