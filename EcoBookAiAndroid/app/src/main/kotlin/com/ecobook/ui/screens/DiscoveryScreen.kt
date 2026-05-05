package com.ecobook.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ecobook.model.Disciplina
import com.ecobook.model.NivelEnsino
import com.ecobook.ui.EcoBookUiState
import com.ecobook.ui.components.FilterChipCard
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.MaterialHighlightCard
import com.ecobook.ui.components.SectionHeading

@Composable
fun DiscoveryScreen(
    uiState: EcoBookUiState,
    onQueryChange: (String) -> Unit,
    onToggleDisciplina: (Disciplina) -> Unit,
    onToggleNivelEnsino: (NivelEnsino) -> Unit,
    onClearFilters: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SectionHeading(
                title = "Descoberta de materiais",
                subtitle = "Catalogo Android pronto para refletir as regras do matching: disciplina, nivel, sistema e proximidade."
            )
        }

        item {
            GlassCard {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onQueryChange,
                    label = { Text("Buscar por titulo, resumo ou local") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Disciplina",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Disciplina.entries.forEach { disciplina ->
                            FilterChipCard(
                                label = disciplina.label,
                                selected = uiState.selectedDisciplina == disciplina,
                                onClick = { onToggleDisciplina(disciplina) }
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Nivel de ensino",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NivelEnsino.entries.forEach { nivel ->
                            FilterChipCard(
                                label = nivel.label,
                                selected = uiState.selectedNivelEnsino == nivel,
                                onClick = { onToggleNivelEnsino(nivel) }
                            )
                        }
                    }
                }
                OutlinedButton(onClick = onClearFilters) {
                    Text("Limpar filtros")
                }
            }
        }

        item {
            Text(
                text = "${uiState.filteredMaterials.size} materiais alinhados com os filtros atuais.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (uiState.filteredMaterials.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = "Nenhum material encontrado.",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Tente remover alguns filtros para ampliar a busca e validar o comportamento do ranking depois.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.filteredMaterials, key = { material -> material.id }) { material ->
                MaterialHighlightCard(material)
            }
        }
    }
}
