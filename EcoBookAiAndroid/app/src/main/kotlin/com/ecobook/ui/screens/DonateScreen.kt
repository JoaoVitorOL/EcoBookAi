package com.ecobook.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ecobook.model.AiAssistStatus
import com.ecobook.ui.EcoBookUiState
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.JourneyStepCard
import com.ecobook.ui.components.SectionHeading
import com.ecobook.ui.components.StatusBadge

@Composable
fun DonateScreen(
    uiState: EcoBookUiState,
    onRefreshBackend: () -> Unit
) {
    LazyColumn(
        modifier = Modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SectionHeading(
                title = "Fluxo de doacao",
                subtitle = "Este modulo ainda funciona como prototipo visual do fluxo de fotos, revisao de IA e publicacao enquanto os endpoints de materiais entram em implementacao."
            )
        }

        item {
            GlassCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val badgeColors = when (uiState.donationPreview.aiStatus) {
                        AiAssistStatus.SUCCESS -> androidx.compose.ui.graphics.Color(0xFFE0EFE4) to androidx.compose.ui.graphics.Color(0xFF205447)
                        AiAssistStatus.LOW_CONFIDENCE -> androidx.compose.ui.graphics.Color(0xFFFCE7D8) to androidx.compose.ui.graphics.Color(0xFF8A4C1F)
                        AiAssistStatus.FAILURE -> androidx.compose.ui.graphics.Color(0xFFF7DDDB) to androidx.compose.ui.graphics.Color(0xFF8D3D30)
                    }
                    StatusBadge(
                        text = uiState.donationPreview.aiStatus.label,
                        containerColor = badgeColors.first,
                        contentColor = badgeColors.second
                    )
                    StatusBadge(
                        text = "Confianca ${(uiState.donationPreview.confidence * 100).toInt()}%",
                        containerColor = androidx.compose.ui.graphics.Color(0xFFEDE8FA),
                        contentColor = androidx.compose.ui.graphics.Color(0xFF4D3B8A)
                    )
                }
                Text(
                    text = uiState.donationPreview.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                uiState.donationPreview.fields.forEach { field ->
                    Text(
                        text = "${field.label}: ${field.value}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item {
            GlassCard {
                Text(
                    text = "Status da integracao",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "O backend ja responde health, auth, perfil e agora tambem reserva a rota /materiais/preview como skeleton. A logica real de preview e publicacao ainda esta pendente.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = onRefreshBackend) {
                    Text("Revalidar backend")
                }
            }
        }

        itemsIndexed(uiState.donationPreview.steps) { index, step ->
            JourneyStepCard(step = step, index = index)
        }
    }
}
