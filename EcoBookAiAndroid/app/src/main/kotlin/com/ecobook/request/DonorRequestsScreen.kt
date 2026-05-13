package com.ecobook.request

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecobook.ui.components.FilterChipCard
import com.ecobook.ui.components.GlassCard

@Composable
fun DonorRequestsScreen(
    topPadding: PaddingValues = PaddingValues(),
    viewModel: DonorRequestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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
            Text(
                text = "Aprove ou recuse novas solicitacoes e finalize as reservas que ja viraram entrega.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            GlassCard {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DonorRequestsTab.entries.forEach { tab ->
                        FilterChipCard(
                            label = tab.label,
                            selected = uiState.selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) }
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
                        text = "Nao foi possivel carregar os pedidos recebidos.",
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

        if (uiState.isLoading && uiState.pendingRequests.isEmpty() && uiState.approvedRequests.isEmpty()) {
            item {
                GlassCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text(
                            text = "Carregando pedidos recebidos...",
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
                        text = when (uiState.selectedTab) {
                            DonorRequestsTab.PENDING -> "Nenhum pedido pendente por enquanto."
                            DonorRequestsTab.APPROVED -> "Nenhuma reserva aprovada aguardando conclusao."
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Assim que outros usuarios solicitarem os seus materiais, o acompanhamento passa a aparecer aqui.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.visibleRequests, key = { it.id }) { request ->
                when (uiState.selectedTab) {
                    DonorRequestsTab.PENDING -> DonorRequestCard(
                        request = request,
                        isWorking = uiState.activeRequestId == request.id,
                        onApprove = { viewModel.approveRequest(request.id) },
                        onDecline = { viewModel.declineRequest(request.id) }
                    )

                    DonorRequestsTab.APPROVED -> DonorRequestCard(
                        request = request,
                        isWorking = uiState.activeRequestId == request.id,
                        onComplete = { viewModel.completeDonation(request.id) },
                        onRevokeApproval = { viewModel.revokeApproval(request.id) }
                    )
                }
            }
        }
    }
}
