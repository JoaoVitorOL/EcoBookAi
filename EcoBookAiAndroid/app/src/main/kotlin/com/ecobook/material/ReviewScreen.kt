@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.ecobook.material

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ecobook.model.AiAssistStatus
import com.ecobook.model.Disciplina
import com.ecobook.model.EstadoConservacao
import com.ecobook.model.NecessidadeAcademica
import com.ecobook.model.NivelEnsino
import com.ecobook.model.SistemaEnsino
import com.ecobook.ui.ConfidenceIndicator
import com.ecobook.ui.EditableField
import com.ecobook.ui.EnumDropdown
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.StatusBadge
import com.ecobook.ui.theme.EcoBookTone
import com.ecobook.ui.theme.ecoBookBadgeColors

@Composable
fun ReviewScreen(
    uiState: MaterialUploadUiState,
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
    onNecessidadeAcademicaChange: (NecessidadeAcademica?) -> Unit,
    onPrepareConfirm: () -> Boolean,
    onRestart: () -> Unit,
    onConfirm: () -> Unit
) {
    var showConfirmation by remember { mutableStateOf(false) }
    var showRestartConfirmation by remember { mutableStateOf(false) }
    val fieldRequesters = remember {
        mapOf(
            "upload_id" to BringIntoViewRequester(),
            "titulo" to BringIntoViewRequester(),
            "autor" to BringIntoViewRequester(),
            "editora" to BringIntoViewRequester(),
            "descricao" to BringIntoViewRequester(),
            "disciplina" to BringIntoViewRequester(),
            "nivel_ensino" to BringIntoViewRequester(),
            "ano" to BringIntoViewRequester(),
            "sistema_ensino" to BringIntoViewRequester(),
            "estado_conservacao" to BringIntoViewRequester(),
            "necessidade_academica" to BringIntoViewRequester(),
            "data_publicacao" to BringIntoViewRequester()
        )
    }
    val firstInvalidField = remember(uiState.validationErrors) {
        listOf(
            "upload_id",
            "titulo",
            "autor",
            "editora",
            "descricao",
            "disciplina",
            "nivel_ensino",
            "ano",
            "sistema_ensino",
            "estado_conservacao",
            "necessidade_academica",
            "data_publicacao"
        ).firstOrNull(uiState.validationErrors::containsKey)
    }
    val overallColors = ecoBookBadgeColors(
        when (uiState.overallStatus) {
            AiAssistStatus.SUCCESS -> EcoBookTone.Success
            AiAssistStatus.LOW_CONFIDENCE -> EcoBookTone.Warning
            AiAssistStatus.FAILURE, null -> EcoBookTone.Neutral
        }
    )
    val uploadIdColors = ecoBookBadgeColors(EcoBookTone.Accent)

    LaunchedEffect(firstInvalidField) {
        firstInvalidField?.let { field ->
            fieldRequesters[field]?.bringIntoView()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        GlassCard(
            modifier = Modifier.bringIntoViewRequester(fieldRequesters.getValue("upload_id"))
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.selectedFrontImage?.let { image ->
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Capa da frente",
                            style = MaterialTheme.typography.titleSmall
                        )
                        AsyncImage(
                            model = image.uri,
                            contentDescription = "Imagem frontal do material selecionado",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(22.dp))
                        )
                    }
                }
                uiState.selectedBackImage?.let { image ->
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Capa de tras",
                            style = MaterialTheme.typography.titleSmall
                        )
                        AsyncImage(
                            model = image.uri,
                            contentDescription = "Imagem traseira do material selecionado",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(22.dp))
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(
                    text = when (uiState.overallStatus) {
                        AiAssistStatus.SUCCESS -> "IA confiante"
                        AiAssistStatus.LOW_CONFIDENCE -> "Revisao recomendada"
                        AiAssistStatus.FAILURE, null -> "Preenchimento manual"
                    },
                    containerColor = overallColors.containerColor,
                    contentColor = overallColors.contentColor
                )
                uiState.uploadId?.let { uploadId ->
                    StatusBadge(
                        text = uploadId.takeLast(8),
                        containerColor = uploadIdColors.containerColor,
                        contentColor = uploadIdColors.contentColor
                    )
                }
                if (uiState.selectedBackImage != null) {
                    StatusBadge(
                        text = "verso incluido",
                        containerColor = uploadIdColors.containerColor,
                        contentColor = uploadIdColors.contentColor
                    )
                }
            }

            Text(
                text = uiState.previewMessage ?: "Revise os campos e ajuste tudo o que for necessario antes de publicar.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            uiState.validationErrors["upload_id"]?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        GlassCard {
            Text(
                text = "Metadados do material",
                style = MaterialTheme.typography.titleLarge
            )

            ConfidenceIndicator(confidence = uiState.confidenceByField["titulo"])
            EditableField(
                value = uiState.draft.titulo,
                onValueChange = onTituloChange,
                label = "Titulo",
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(fieldRequesters.getValue("titulo")),
                errorMessage = uiState.validationErrors["titulo"]
            )

            ConfidenceIndicator(confidence = uiState.confidenceByField["autor"])
            EditableField(
                value = uiState.draft.autor,
                onValueChange = onAutorChange,
                label = "Autor (opcional)",
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(fieldRequesters.getValue("autor")),
                errorMessage = uiState.validationErrors["autor"]
            )

            ConfidenceIndicator(confidence = uiState.confidenceByField["editora"])
            EditableField(
                value = uiState.draft.editora,
                onValueChange = onEditoraChange,
                label = "Editora (opcional)",
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(fieldRequesters.getValue("editora")),
                errorMessage = uiState.validationErrors["editora"]
            )

            ConfidenceIndicator(confidence = null)
            EditableField(
                value = uiState.draft.descricao,
                onValueChange = onDescricaoChange,
                label = "Descricao",
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(fieldRequesters.getValue("descricao")),
                errorMessage = uiState.validationErrors["descricao"],
                singleLine = false,
                minLines = 4
            )

            ConfidenceIndicator(confidence = uiState.confidenceByField["disciplina"])
            EnumDropdown(
                label = "Disciplina",
                selectedValue = uiState.draft.disciplina,
                options = uiState.disciplinas,
                optionLabel = { it.label },
                onSelected = { onDisciplinaChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(fieldRequesters.getValue("disciplina")),
                errorMessage = uiState.validationErrors["disciplina"]
            )

            ConfidenceIndicator(confidence = uiState.confidenceByField["nivel_ensino"])
            EnumDropdown(
                label = "Nivel de ensino",
                selectedValue = uiState.draft.nivelEnsino,
                options = uiState.niveisEnsino,
                optionLabel = { it.label },
                onSelected = { onNivelEnsinoChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(fieldRequesters.getValue("nivel_ensino")),
                errorMessage = uiState.validationErrors["nivel_ensino"]
            )

            ConfidenceIndicator(confidence = uiState.confidenceByField["ano"])
            EditableField(
                value = uiState.draft.ano,
                onValueChange = onAnoChange,
                label = "Ano escolar",
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(fieldRequesters.getValue("ano")),
                errorMessage = uiState.validationErrors["ano"],
                supportingMessage = when (uiState.draft.nivelEnsino) {
                    NivelEnsino.MEDIO -> "Para ensino medio, use apenas 1, 2 ou 3."
                    NivelEnsino.SUPERIOR -> "Nao se aplica a materiais de ensino superior."
                    else -> "Para ensino fundamental, use um valor de 1 a 9."
                },
                enabled = uiState.draft.nivelEnsino != NivelEnsino.SUPERIOR
            )

            ConfidenceIndicator(confidence = uiState.confidenceByField["sistema_ensino"])
            EnumDropdown(
                label = "Sistema de ensino",
                selectedValue = uiState.draft.sistemaEnsino,
                options = uiState.sistemasEnsino,
                optionLabel = { it.label },
                onSelected = { onSistemaEnsinoChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(fieldRequesters.getValue("sistema_ensino")),
                errorMessage = uiState.validationErrors["sistema_ensino"]
            )

            ConfidenceIndicator(confidence = null)
            EnumDropdown(
                label = "Estado de conservacao",
                selectedValue = uiState.draft.estadoConservacao,
                options = uiState.estadosConservacao,
                optionLabel = { it.label },
                onSelected = { onEstadoConservacaoChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(fieldRequesters.getValue("estado_conservacao")),
                errorMessage = uiState.validationErrors["estado_conservacao"]
            )

            ConfidenceIndicator(confidence = null)
            EnumDropdown(
                label = "Necessidade academica",
                selectedValue = uiState.draft.necessidadeAcademica,
                options = uiState.necessidadesAcademicas,
                optionLabel = { it.label },
                onSelected = { onNecessidadeAcademicaChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(fieldRequesters.getValue("necessidade_academica")),
                errorMessage = uiState.validationErrors["necessidade_academica"]
            )

            ConfidenceIndicator(confidence = uiState.confidenceByField["data_publicacao"])
            EditableField(
                value = uiState.draft.dataPublicacao,
                onValueChange = onDataPublicacaoChange,
                label = "Ano de publicacao",
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(fieldRequesters.getValue("data_publicacao")),
                errorMessage = uiState.validationErrors["data_publicacao"]
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showRestartConfirmation = true }
                ) {
                    Text("Trocar imagens")
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (onPrepareConfirm()) {
                            showConfirmation = true
                        }
                    },
                    enabled = uiState.canSubmit
                ) {
                    Text(if (uiState.isBusy) "Publicando..." else "Publicar material")
                }
            }
        }
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Confirmar material") },
            text = {
                Text(
                    "Voce vai publicar \"${uiState.draft.titulo.ifBlank { "Material sem titulo" }}\" como DISPONIVEL. Ainda podera ajustar o fluxo nas fases seguintes, mas esta publicacao ja entra pronta para matching."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmation = false
                        onConfirm()
                    },
                    enabled = !uiState.isBusy
                ) {
                    Text(if (uiState.isBusy) "Publicando..." else "Confirmar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmation = false }) {
                    Text("Editar")
                }
            }
        )
    }

    if (showRestartConfirmation) {
        AlertDialog(
            onDismissRequest = { showRestartConfirmation = false },
            title = { Text("Descartar revisao") },
            text = {
                Text("Voce vai apagar a revisao atual e voltar para a etapa de escolha das imagens. Use isso apenas se realmente quiser recomecar.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestartConfirmation = false
                        onRestart()
                    }
                ) {
                    Text("Voltar e trocar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestartConfirmation = false }) {
                    Text("Continuar revisando")
                }
            }
        )
    }
}
