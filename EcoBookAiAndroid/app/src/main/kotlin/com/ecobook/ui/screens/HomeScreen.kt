package com.ecobook.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ecobook.model.BackendConnectionState
import com.ecobook.ui.EcoBookUiState
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.InsightCard
import com.ecobook.ui.components.MetricCard
import com.ecobook.ui.components.SectionHeading
import com.ecobook.ui.components.StatusBadge
import com.ecobook.ui.theme.backendConnectionBadgeColors
import com.ecobook.ui.theme.ecoBookHeroBrush
import com.ecobook.ui.theme.ecoBookHeroContentColor

@Composable
fun HomeScreen(
    uiState: EcoBookUiState,
    onRefreshBackend: () -> Unit,
    onOpenDiscovery: () -> Unit,
    onOpenDonate: () -> Unit,
    onOpenMyRequests: () -> Unit,
    onOpenDonorRequests: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val heroContentColor = ecoBookHeroContentColor()
    val backendColors = backendConnectionBadgeColors(uiState.backendStatus.state)

    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = ecoBookHeroBrush(),
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "EcoBook AI",
                        style = MaterialTheme.typography.displayLarge,
                        color = heroContentColor
                    )
                    Text(
                        text = "Transformamos o que já existe no repositório em um app Android pronto para evoluir com o backend.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = heroContentColor.copy(alpha = 0.92f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = onOpenDiscovery) {
                            Text("Buscar materiais")
                        }
                        OutlinedButton(onClick = onOpenDonate) {
                            Text("Fluxo de doação")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = onOpenMyRequests) {
                            Text("Minhas solicitações")
                        }
                        OutlinedButton(onClick = onOpenDonorRequests) {
                            Text("Pedidos recebidos")
                        }
                    }
                    OutlinedButton(onClick = onOpenProfile) {
                        Text("Perfil e onboarding")
                    }
                }
            }
        }

        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = uiState.backendStatus.headline,
                            style = MaterialTheme.typography.titleLarge
                        )
                        StatusBadge(
                            text = when (uiState.backendStatus.state) {
                                BackendConnectionState.ONLINE -> "ONLINE"
                                BackendConnectionState.CHECKING -> "CHECKING"
                                BackendConnectionState.OFFLINE -> "OFFLINE"
                            },
                            containerColor = backendColors.containerColor,
                            contentColor = backendColors.contentColor
                        )
                    }
                    OutlinedButton(onClick = onRefreshBackend) {
                        Text("Atualizar")
                    }
                }
                Text(
                    text = uiState.backendStatus.detail,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                uiState.backendStatus.version?.let { version ->
                    Text(
                        text = "Versão reportada: $version",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Discovery",
                    value = "Live API",
                    description = "Busca paginada e filtros agora falam com /v1/materiais.",
                    icon = Icons.Rounded.MenuBook,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Perfil",
                    value = "${uiState.profile.completionPercent}%",
                    description = "Prontidão do onboarding local para integrar com /usuarios.",
                    icon = Icons.Rounded.AccountCircle,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            MetricCard(
                title = "API real conectada",
                value = "/v1/health",
                description = "Ponto de integração já validado entre Android e Spring Boot.",
                icon = Icons.Rounded.CloudDone
            )
        }

        item {
            SectionHeading(
                title = "Raio-X do projeto atual",
                subtitle = "Resumo da análise do que já existia e do que agora ficou pronto para o Android Studio."
            )
        }

        items(uiState.insights) { insight ->
            InsightCard(insight)
        }
    }
}
