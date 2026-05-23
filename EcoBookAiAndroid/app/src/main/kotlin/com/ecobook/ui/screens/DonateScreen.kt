package com.ecobook.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecobook.discovery.MaterialImage
import com.ecobook.discovery.formatAnoEscolar
import com.ecobook.discovery.formatDisciplina
import com.ecobook.discovery.formatEstadoConservacao
import com.ecobook.discovery.formatNivelEnsino
import com.ecobook.discovery.formatRelativeDate
import com.ecobook.dto.MaterialDTO
import com.ecobook.model.Disciplina
import com.ecobook.model.EstadoConservacao
import com.ecobook.model.NivelEnsino
import com.ecobook.model.SistemaEnsino
import com.ecobook.ui.EditableField
import com.ecobook.ui.EnumDropdown
import com.ecobook.ui.components.FilterChipCard
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.SectionHeading
import com.ecobook.ui.components.StatusBadge

private enum class DonateMode {
    HISTORY,
    PUBLISH
}

@Composable
fun DonateScreen(
    onOpenDonorRequests: () -> Unit = {},
    onOpenPublishNew: () -> Unit = {},
    unreadNotifications: Int = 0,
    onOpenNotifications: () -> Unit = {},
    viewModel: DonateViewModel = hiltViewModel()
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
                viewModel.refreshMaterials()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DonateHistoryContent(
        uiState = uiState,
        onOpenEditor = viewModel::openEditor,
        onDelete = viewModel::promptDelete,
        onSwitchToPublish = onOpenPublishNew,
        onOpenDonorRequests = onOpenDonorRequests,
        unreadNotifications = unreadNotifications,
        onOpenNotifications = onOpenNotifications
    )

    uiState.materialBeingEdited?.let { material ->
        EditMaterialDialog(
            material = material,
            uiState = uiState,
            onDismiss = viewModel::dismissEditor,
            onTituloChange = viewModel::updateTitulo,
            onAutorChange = viewModel::updateAutor,
            onEditoraChange = viewModel::updateEditora,
            onDescricaoChange = viewModel::updateDescricao,
            onAnoChange = viewModel::updateAno,
            onDataPublicacaoChange = viewModel::updateDataPublicacao,
            onDisciplinaChange = viewModel::updateDisciplina,
            onNivelEnsinoChange = viewModel::updateNivelEnsino,
            onSistemaEnsinoChange = viewModel::updateSistemaEnsino,
            onEstadoConservacaoChange = viewModel::updateEstadoConservacao,
            onSave = viewModel::saveEditedMaterial
        )
    }

    uiState.pendingDeleteMaterial?.let { material ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeletePrompt,
            title = { Text("Excluir material") },
            text = {
                Text(
                    "O material \"${material.titulo}\" será removido da sua conta e deixará de aparecer na busca pública."
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::deletePendingMaterial,
                    enabled = !uiState.isDeleting,
                    colors = destructiveButtonColors()
                ) {
                    Text(if (uiState.isDeleting) "Excluindo..." else "Excluir")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::dismissDeletePrompt) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun DonateHistoryContent(
    uiState: DonateUiState,
    onOpenEditor: (MaterialDTO) -> Unit,
    onDelete: (MaterialDTO) -> Unit,
    onSwitchToPublish: () -> Unit,
    onOpenDonorRequests: () -> Unit,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit
) {
    val visibleMaterials = uiState.visibleMaterials

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SectionHeading(
                title = "Área do doador",
                subtitle = "Acompanhe tudo o que você já publicou, edite materiais disponíveis e abra novos cadastros quando quiser.",
                trailingContent = {
                    com.ecobook.ui.components.NotificationsEntryPointButton(
                        unreadCount = unreadNotifications,
                        onClick = onOpenNotifications
                    )
                }
            )
        }

        item {
            DonateModeSwitchCard(
                selectedMode = DonateMode.HISTORY,
                onShowHistory = {},
                onShowPublish = onSwitchToPublish,
                onOpenDonorRequests = onOpenDonorRequests
            )
        }

        if (visibleMaterials.isNotEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = "Você tem ${visibleMaterials.size} materiais ativos cadastrados nesta conta.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        uiState.errorMessage?.let { message ->
            item {
                GlassCard {
                    Text(
                        text = "Não foi possível carregar seus materiais.",
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

        if (uiState.isLoading && visibleMaterials.isEmpty()) {
            item {
                GlassCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text(
                            text = "Carregando seus materiais publicados...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (visibleMaterials.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = "Nenhum material publicado por enquanto.",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Use o modo de publicação para cadastrar seu primeiro material e fazê-lo aparecer aqui.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onSwitchToPublish) {
                        Text("Publicar material")
                    }
                }
            }
        } else {
            items(visibleMaterials, key = { it.id }) { material ->
                DonateMaterialCard(
                    material = material,
                    onEdit = { onOpenEditor(material) },
                    onDelete = { onDelete(material) }
                )
            }
        }
    }
}

@Composable
private fun DonateModeSwitchCard(
    selectedMode: DonateMode,
    onShowHistory: () -> Unit,
    onShowPublish: () -> Unit,
    onOpenDonorRequests: () -> Unit
) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChipCard(
                label = "Meus materiais",
                selected = selectedMode == DonateMode.HISTORY,
                onClick = onShowHistory,
                modifier = Modifier.weight(1f),
                minLines = 2,
                maxLines = 2,
                textAlign = TextAlign.Center
            )
            FilterChipCard(
                label = "Pedidos recebidos",
                selected = false,
                onClick = onOpenDonorRequests,
                modifier = Modifier.weight(1f),
                minLines = 2,
                maxLines = 2,
                textAlign = TextAlign.Center
            )
            FilterChipCard(
                label = "Publicar novo",
                selected = selectedMode == DonateMode.PUBLISH,
                onClick = onShowPublish,
                modifier = Modifier.weight(1f),
                minLines = 2,
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DonateMaterialCard(
    material: MaterialDTO,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isAvailable = material.status == "DISPONIVEL"
    val statusColors = statusColors(material.status)

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MaterialImage(
                imageUrl = material.imagemUrl,
                title = material.titulo,
                modifier = Modifier
                    .width(90.dp)
                    .height(126.dp)
            )

            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = material.titulo,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = material.descricao,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        text = humanizeStatus(material.status),
                        containerColor = statusColors.first,
                        contentColor = statusColors.second
                    )
                    StatusBadge(
                        text = formatDisciplina(material.disciplina),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = "${formatNivelEnsino(material.nivelEnsino)} | ${formatAnoEscolar(material.ano)} | ${formatEstadoConservacao(material.estadoConservacao)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Cadastrado ${formatRelativeDate(material.criadoEm) ?: "recentemente"}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isAvailable) {
                    Text(
                        text = "Edição e exclusão ficam disponíveis apenas enquanto o material estiver DISPONÍVEL.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onEdit,
                        enabled = isAvailable,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Editar")
                    }
                    Button(
                        onClick = onDelete,
                        enabled = isAvailable,
                        colors = destructiveButtonColors(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Excluir")
                    }
                }
            }
        }
    }
}

@Composable
private fun destructiveButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.errorContainer,
    contentColor = MaterialTheme.colorScheme.onErrorContainer,
    disabledContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.38f),
    disabledContentColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.38f)
)

@Composable
private fun EditMaterialDialog(
    material: MaterialDTO,
    uiState: DonateUiState,
    onDismiss: () -> Unit,
    onTituloChange: (String) -> Unit,
    onAutorChange: (String) -> Unit,
    onEditoraChange: (String) -> Unit,
    onDescricaoChange: (String) -> Unit,
    onAnoChange: (String) -> Unit,
    onDataPublicacaoChange: (String) -> Unit,
    onDisciplinaChange: (Disciplina?) -> Unit,
    onNivelEnsinoChange: (NivelEnsino?) -> Unit,
    onSistemaEnsinoChange: (SistemaEnsino?) -> Unit,
    onEstadoConservacaoChange: (EstadoConservacao?) -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar material") },
        text = {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Capa da frente",
                            style = MaterialTheme.typography.titleSmall
                        )
                        MaterialImage(
                            imageUrl = material.imagemUrl,
                            title = material.titulo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                    }
                    material.imagemVersoUrl?.let { backImageUrl ->
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Capa de trás",
                                style = MaterialTheme.typography.titleSmall
                            )
                            MaterialImage(
                                imageUrl = backImageUrl,
                                title = "${material.titulo} verso",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            )
                        }
                    }
                }
                uiState.editorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                EditableField(
                    value = uiState.editDraft.titulo,
                    onValueChange = onTituloChange,
                    label = "Título",
                    modifier = Modifier.fillMaxWidth(),
                    errorMessage = uiState.validationErrors["titulo"]
                )
                EditableField(
                    value = uiState.editDraft.autor,
                    onValueChange = onAutorChange,
                    label = "Autor (opcional)",
                    modifier = Modifier.fillMaxWidth(),
                    errorMessage = uiState.validationErrors["autor"]
                )
                EditableField(
                    value = uiState.editDraft.editora,
                    onValueChange = onEditoraChange,
                    label = "Editora (opcional)",
                    modifier = Modifier.fillMaxWidth(),
                    errorMessage = uiState.validationErrors["editora"]
                )
                EditableField(
                    value = uiState.editDraft.descricao,
                    onValueChange = onDescricaoChange,
                    label = "Descrição",
                    modifier = Modifier.fillMaxWidth(),
                    errorMessage = uiState.validationErrors["descricao"],
                    singleLine = false,
                    minLines = 4
                )
                EnumDropdown(
                    label = "Disciplina",
                    selectedValue = uiState.editDraft.disciplina,
                    options = uiState.disciplinas,
                    optionLabel = { it.label },
                    onSelected = { onDisciplinaChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    errorMessage = uiState.validationErrors["disciplina"]
                )
                EnumDropdown(
                    label = "Nível de ensino",
                    selectedValue = uiState.editDraft.nivelEnsino,
                    options = uiState.niveisEnsino,
                    optionLabel = { it.label },
                    onSelected = { onNivelEnsinoChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    errorMessage = uiState.validationErrors["nivel_ensino"]
                )
                EditableField(
                    value = uiState.editDraft.ano,
                    onValueChange = onAnoChange,
                    label = "Ano escolar",
                    modifier = Modifier.fillMaxWidth(),
                    errorMessage = uiState.validationErrors["ano"],
                    supportingMessage = when (uiState.editDraft.nivelEnsino) {
                        NivelEnsino.MEDIO -> "Para ensino médio, use apenas 1, 2 ou 3."
                        NivelEnsino.SUPERIOR -> "Não se aplica a materiais de ensino superior."
                        else -> "Para ensino fundamental, use um valor de 1 a 9."
                    },
                    enabled = uiState.editDraft.nivelEnsino != NivelEnsino.SUPERIOR
                )
                EnumDropdown(
                    label = "Sistema de ensino",
                    selectedValue = uiState.editDraft.sistemaEnsino,
                    options = uiState.sistemasEnsino,
                    optionLabel = { it.label },
                    onSelected = { onSistemaEnsinoChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    errorMessage = uiState.validationErrors["sistema_ensino"]
                )
                EnumDropdown(
                    label = "Estado de conservação",
                    selectedValue = uiState.editDraft.estadoConservacao,
                    options = uiState.estadosConservacao,
                    optionLabel = { it.label },
                    onSelected = { onEstadoConservacaoChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    errorMessage = uiState.validationErrors["estado_conservacao"]
                )
                EditableField(
                    value = uiState.editDraft.dataPublicacao,
                    onValueChange = onDataPublicacaoChange,
                    label = "Ano de publicação",
                    modifier = Modifier.fillMaxWidth(),
                    errorMessage = uiState.validationErrors["data_publicacao"]
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving
            ) {
                Text(if (uiState.isSaving) "Salvando..." else "Salvar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun humanizeStatus(value: String): String {
    return when (value) {
        "DISPONIVEL" -> "Disponível"
        "RESERVADO" -> "Reservado"
        "DOADO" -> "Doado"
        "CANCELADO" -> "Cancelado"
        else -> value
    }
}

private fun statusColors(status: String): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> {
    return when (status) {
        "DISPONIVEL" -> androidx.compose.ui.graphics.Color(0xFFE5F0EA) to androidx.compose.ui.graphics.Color(0xFF205447)
        "RESERVADO" -> androidx.compose.ui.graphics.Color(0xFFFCE7D8) to androidx.compose.ui.graphics.Color(0xFF8A4C1F)
        "DOADO" -> androidx.compose.ui.graphics.Color(0xFFE6EEF8) to androidx.compose.ui.graphics.Color(0xFF214A73)
        "CANCELADO" -> androidx.compose.ui.graphics.Color(0xFFF0F1F3) to androidx.compose.ui.graphics.Color(0xFF4B5563)
        else -> androidx.compose.ui.graphics.Color(0xFFF0F1F3) to androidx.compose.ui.graphics.Color(0xFF4B5563)
    }
}
