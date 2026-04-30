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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ecobook.model.BackendConnectionState
import com.ecobook.ui.EcoBookUiState
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.InsightCard
import com.ecobook.ui.components.MetricCard
import com.ecobook.ui.components.SectionHeading
import com.ecobook.ui.components.StatusBadge

@Composable
fun HomeScreen(
    uiState: EcoBookUiState,
    onRefreshBackend: () -> Unit,
    onOpenDiscovery: () -> Unit,
    onOpenDonate: () -> Unit,
    onOpenProfile: () -> Unit
) {
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
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF235B4A), Color(0xFF4F8666))
                        ),
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "EcoBook AI",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Transformamos o que ja existe no repositorio em um app Android pronto para evoluir com o backend.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = onOpenDiscovery) {
                            Text("Buscar materiais")
                        }
                        OutlinedButton(onClick = onOpenDonate) {
                            Text("Fluxo de doacao")
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
                        val badgeColors = when (uiState.backendStatus.state) {
                            BackendConnectionState.ONLINE -> Color(0xFFE0EFE4) to Color(0xFF205447)
                            BackendConnectionState.CHECKING -> Color(0xFFFCE7D8) to Color(0xFF8A4C1F)
                            BackendConnectionState.OFFLINE -> Color(0xFFF7DDDB) to Color(0xFF8D3D30)
                        }
                        StatusBadge(
                            text = when (uiState.backendStatus.state) {
                                BackendConnectionState.ONLINE -> "ONLINE"
                                BackendConnectionState.CHECKING -> "CHECKING"
                                BackendConnectionState.OFFLINE -> "OFFLINE"
                            },
                            containerColor = badgeColors.first,
                            contentColor = badgeColors.second
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
                        text = "Versao reportada: $version",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Catalogo base",
                    value = uiState.catalog.size.toString(),
                    description = "Materiais de exemplo alinhados ao matching do projeto.",
                    icon = Icons.Rounded.MenuBook,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Perfil",
                    value = "${uiState.profile.completionPercent}%",
                    description = "Prontidao do onboarding local para integrar com /usuarios.",
                    icon = Icons.Rounded.AccountCircle,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            MetricCard(
                title = "API real conectada",
                value = "/v1/health",
                description = "Ponto de integracao ja validado entre Android e Spring Boot.",
                icon = Icons.Rounded.CloudDone
            )
        }

        item {
            SectionHeading(
                title = "Raio-X do projeto atual",
                subtitle = "Resumo da analise do que ja existia e do que agora ficou pronto para o Android Studio."
            )
        }

        items(uiState.insights) { insight ->
            InsightCard(insight)
        }
    }
}
