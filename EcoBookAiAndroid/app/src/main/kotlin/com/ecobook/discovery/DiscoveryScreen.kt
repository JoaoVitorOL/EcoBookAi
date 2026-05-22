package com.ecobook.discovery

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ecobook.model.Disciplina
import com.ecobook.model.NivelEnsino
import com.ecobook.model.SistemaEnsino
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.SectionHeading
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Composable
fun DiscoveryScreen(
    onOpenMyRequests: () -> Unit = {},
    unreadNotifications: Int = 0,
    onOpenNotifications: () -> Unit = {},
    viewModel: DiscoveryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    LaunchedEffect(uiState.pendingNavigation) {
        if (uiState.pendingNavigation == DiscoveryNavigation.MY_REQUESTS) {
            onOpenMyRequests()
            viewModel.consumeNavigation()
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshActiveSearch()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(listState, uiState.results.size, uiState.hasNext, uiState.isLoadingMore) {
        snapshotFlow {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 4
        }
            .map { shouldLoad -> shouldLoad && uiState.hasNext && !uiState.isLoadingMore }
            .distinctUntilChanged()
            .filter { it }
            .collect { viewModel.loadNextPage() }
    }

    uiState.selectedMaterial?.let { material ->
        MaterialDetailDialog(
            material = material,
            isRequestInFlight = uiState.requestingMaterialId == material.id,
            onDismiss = viewModel::closeMaterialDetail,
            onRequestMaterial = { viewModel.requestMaterial(material.id) }
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SectionHeading(
                title = "Descoberta de materiais",
                subtitle = "Busca real conectada ao backend com matching deterministico, filtros academicos e ordenacao por proximidade.",
                trailingContent = {
                    com.ecobook.ui.components.NotificationsEntryPointButton(
                        unreadCount = unreadNotifications,
                        onClick = onOpenNotifications
                    )
                }
            )
        }

        item {
            DiscoveryFiltersCard(
                uiState = uiState,
                onQueryChange = viewModel::updateQuery,
                onDisciplinaChange = viewModel::updateDisciplina,
                onNivelEnsinoChange = viewModel::updateNivelEnsino,
                onAnoChange = viewModel::updateAno,
                onSistemaEnsinoChange = viewModel::updateSistemaEnsino,
                onCidadeChange = viewModel::updateCidade,
                onBairroChange = viewModel::updateBairro,
                onMinAnoPublicacaoChange = viewModel::updateMinAnoPublicacao,
                onMaxAnoPublicacaoChange = viewModel::updateMaxAnoPublicacao,
                onSearch = viewModel::search,
                onReset = viewModel::resetFilters,
                isLoading = uiState.isLoadingInitial
            )
        }

        item {
            ResultsSummary(uiState = uiState)
        }

        uiState.errorMessage?.let { message ->
            item {
                GlassCard {
                    Text(
                        text = "Não foi possível carregar a busca.",
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

        if (uiState.isLoadingInitial && uiState.results.isEmpty()) {
            item {
                GlassCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text(
                            text = "Consultando materiais disponíveis...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (uiState.hasSearched && uiState.results.isEmpty()) {
            item {
                EmptyDiscoveryState(onBrowseAll = viewModel::resetFilters)
            }
        } else {
            items(uiState.results, key = { it.id }) { material ->
                MaterialListItem(
                    material = material,
                    onClick = { viewModel.openMaterialDetail(material) }
                )
            }
        }

        if (uiState.isLoadingMore) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun ResultsSummary(
    uiState: DiscoveryUiState
) {
    val activeCity = uiState.activeFilters.cidade.takeIf { it.isNotBlank() }
    val activeNeighborhood = uiState.activeFilters.bairro.takeIf { it.isNotBlank() }
    val regionLabel = when {
        activeCity != null && activeNeighborhood != null -> "$activeNeighborhood, $activeCity"
        activeCity != null -> activeCity
        else -> "todo o catálogo disponível"
    }

    val summary = if (uiState.total == 0L) {
        "Nenhum material encontrado para $regionLabel."
    } else {
        "${uiState.results.size} de ${uiState.total} materiais carregados para $regionLabel."
    }

    Text(
        text = summary,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun EmptyDiscoveryState(
    onBrowseAll: () -> Unit
) {
    GlassCard {
        Text(
            text = "Nenhum material encontrado.",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Ajuste os filtros ou abra todos os materiais disponíveis para ampliar a busca.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = onBrowseAll) {
            Text("Ver todos os materiais")
        }
    }
}

@Composable
private fun DiscoveryFiltersCard(
    uiState: DiscoveryUiState,
    onQueryChange: (String) -> Unit,
    onDisciplinaChange: (Disciplina?) -> Unit,
    onNivelEnsinoChange: (NivelEnsino?) -> Unit,
    onAnoChange: (String) -> Unit,
    onSistemaEnsinoChange: (SistemaEnsino?) -> Unit,
    onCidadeChange: (String) -> Unit,
    onBairroChange: (String) -> Unit,
    onMinAnoPublicacaoChange: (String) -> Unit,
    onMaxAnoPublicacaoChange: (String) -> Unit,
    onSearch: () -> Unit,
    onReset: () -> Unit,
    isLoading: Boolean
) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Filtros da busca",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (expanded) {
                        "Ajuste disciplina, nível, sistema de ensino e local quando quiser refinar os resultados."
                    } else {
                        collapsedFiltersSummary(uiState)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Minimizar filtros" else "Expandir filtros"
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.filters.query,
                    onValueChange = onQueryChange,
                    label = { Text("Buscar por título, descrição, autor ou local") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                DropdownField(
                    label = "Disciplina",
                    selectedOption = uiState.filters.disciplina,
                    options = Disciplina.entries,
                    allLabel = "Qualquer disciplina",
                    optionLabel = { it.label },
                    onSelected = onDisciplinaChange
                )
                DropdownField(
                    label = "Nivel de ensino",
                    selectedOption = uiState.filters.nivelEnsino,
                    options = NivelEnsino.entries,
                    allLabel = "Todos",
                    optionLabel = { it.label },
                    onSelected = onNivelEnsinoChange
                )
                DropdownField(
                    label = "Sistema de ensino",
                    selectedOption = uiState.filters.sistemaEnsino,
                    options = SistemaEnsino.entries,
                    allLabel = "Todos",
                    optionLabel = { it.label },
                    onSelected = onSistemaEnsinoChange
                )
                OutlinedTextField(
                    value = uiState.filters.ano,
                    onValueChange = onAnoChange,
                    label = { Text("Ano escolar") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = uiState.filters.nivelEnsino != NivelEnsino.SUPERIOR,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        Text(
                            when (uiState.filters.nivelEnsino) {
                                NivelEnsino.MEDIO -> "Use apenas 1, 2 ou 3 para ensino médio."
                                NivelEnsino.SUPERIOR -> "Não se aplica a materiais de ensino superior."
                                else -> "Use um valor de 1 a 9."
                            }
                        )
                    }
                )
                OutlinedTextField(
                    value = uiState.filters.cidade,
                    onValueChange = onCidadeChange,
                    label = { Text("Cidade") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    supportingText = {
                        Text("Opcional. A busca padroniza a cidade sem acento antes de filtrar.")
                    }
                )
                OutlinedTextField(
                    value = uiState.filters.bairro,
                    onValueChange = onBairroChange,
                    label = { Text("Bairro") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.filters.minAnoPublicacao,
                        onValueChange = onMinAnoPublicacaoChange,
                        label = { Text("Publicação de") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = uiState.filters.maxAnoPublicacao,
                        onValueChange = onMaxAnoPublicacaoChange,
                        label = { Text("Publicação até") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onSearch,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Buscar")
                    }
                    OutlinedButton(
                        onClick = onReset,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Resetar")
                    }
                }
            }
        }
    }
}

private fun collapsedFiltersSummary(uiState: DiscoveryUiState): String {
    val activeFilters = buildList {
        uiState.filters.query.takeIf { it.isNotBlank() }?.let { add("texto") }
        uiState.filters.disciplina?.let { add(it.label) }
        uiState.filters.nivelEnsino?.let { add(it.label) }
        uiState.filters.sistemaEnsino?.let { add(it.label) }
        uiState.filters.cidade.takeIf { it.isNotBlank() }?.let { add(it) }
        uiState.filters.bairro.takeIf { it.isNotBlank() }?.let { add(it) }
    }

    return if (activeFilters.isEmpty()) {
        "Filtros recolhidos. Toque para expandir quando quiser refinar a busca."
    } else {
        "Filtros recolhidos com ${activeFilters.size} critério(s) ativo(s): ${activeFilters.joinToString(", ")}."
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownField(
    label: String,
    selectedOption: T?,
    options: List<T>,
    allLabel: String,
    optionLabel: (T) -> String,
    onSelected: (T?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedOption?.let(optionLabel) ?: allLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(allLabel) },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
