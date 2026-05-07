package com.ecobook.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ecobook.model.Disciplina
import com.ecobook.model.EstadoConservacao
import com.ecobook.model.NivelEnsino
import com.ecobook.model.SistemaEnsino
import com.ecobook.ui.ConfidenceIndicator
import com.ecobook.ui.EditableField
import com.ecobook.ui.EnumDropdown
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.StatusBadge

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
    onRestart: () -> Unit,
    onConfirm: () -> Unit
) {
    var showConfirmation by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        GlassCard {
            uiState.selectedImage?.let { image ->
                AsyncImage(
                    model = image.uri,
                    contentDescription = "Imagem do material selecionado",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(22.dp))
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(
                    text = when (uiState.overallStatus) {
                        com.ecobook.model.AiAssistStatus.SUCCESS -> "IA confiante"
                        com.ecobook.model.AiAssistStatus.LOW_CONFIDENCE -> "Revisao recomendada"
                        com.ecobook.model.AiAssistStatus.FAILURE, null -> "Preenchimento manual"
                    },
                    containerColor = when (uiState.overallStatus) {
                        com.ecobook.model.AiAssistStatus.SUCCESS -> androidx.compose.ui.graphics.Color(0xFFE0EFE4)
                        com.ecobook.model.AiAssistStatus.LOW_CONFIDENCE -> androidx.compose.ui.graphics.Color(0xFFFCE7D8)
                        com.ecobook.model.AiAssistStatus.FAILURE, null -> androidx.compose.ui.graphics.Color(0xFFF0F1F3)
                    },
                    contentColor = when (uiState.overallStatus) {
                        com.ecobook.model.AiAssistStatus.SUCCESS -> androidx.compose.ui.graphics.Color(0xFF205447)
                        com.ecobook.model.AiAssistStatus.LOW_CONFIDENCE -> androidx.compose.ui.graphics.Color(0xFF8A4C1F)
                        com.ecobook.model.AiAssistStatus.FAILURE, null -> androidx.compose.ui.graphics.Color(0xFF4B5563)
                    }
                )
                uiState.uploadId?.let { uploadId ->
                    StatusBadge(
                        text = uploadId.takeLast(8),
                        containerColor = androidx.compose.ui.graphics.Color(0xFFEDE8FA),
                        contentColor = androidx.compose.ui.graphics.Color(0xFF4D3B8A)
                    )
                }
            }

            Text(
                text = uiState.previewMessage ?: "Revise os campos e ajuste tudo o que for necessario antes de publicar.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                modifier = Modifier.fillMaxWidth(),
                errorMessage = uiState.validationErrors["titulo"]
            )

            ConfidenceIndicator(confidence = uiState.confidenceByField["autor"])
            EditableField(
                value = uiState.draft.autor,
                onValueChange = onAutorChange,
                label = "Autor (opcional)",
                modifier = Modifier.fillMaxWidth(),
                errorMessage = uiState.validationErrors["autor"]
            )

            ConfidenceIndicator(confidence = uiState.confidenceByField["editora"])
            EditableField(
                value = uiState.draft.editora,
                onValueChange = onEditoraChange,
                label = "Editora (opcional)",
                modifier = Modifier.fillMaxWidth(),
                errorMessage = uiState.validationErrors["editora"]
            )

            ConfidenceIndicator(confidence = null)
            EditableField(
                value = uiState.draft.descricao,
                onValueChange = onDescricaoChange,
                label = "Descricao",
                modifier = Modifier.fillMaxWidth(),
                errorMessage = uiState.validationErrors["descricao"],
                singleLine = false,
                minLines = 4
            )

            ConfidenceIndicator(confidence = uiState.confidenceByField["disciplina"])
            EnumDropdown(
                label = "Disciplina",
                selectedValue = uiState.draft.disciplina,
                options = Disciplina.entries.toList(),
                optionLabel = { it.label },
                onSelected = { onDisciplinaChange(it) },
                modifier = Modifier.fillMaxWidth(),
                errorMessage = uiState.validationErrors["disciplina"]
            )

            ConfidenceIndicator(confidence = uiState.confidenceByField["nivel_ensino"])
            EnumDropdown(
                label = "Nivel de ensino",
                selectedValue = uiState.draft.nivelEnsino,
                options = NivelEnsino.entries.toList(),
                optionLabel = { it.label },
                onSelected = { onNivelEnsinoChange(it) },
                modifier = Modifier.fillMaxWidth(),
                errorMessage = uiState.validationErrors["nivel_ensino"]
            )

            ConfidenceIndicator(confidence = uiState.confidenceByField["ano"])
            EditableField(
                value = uiState.draft.ano,
                onValueChange = onAnoChange,
                label = "Ano escolar",
                modifier = Modifier.fillMaxWidth(),
                errorMessage = uiState.validationErrors["ano"]
            )

            ConfidenceIndicator(confidence = uiState.confidenceByField["sistema_ensino"])
            EnumDropdown(
                label = "Sistema de ensino",
                selectedValue = uiState.draft.sistemaEnsino,
                options = SistemaEnsino.entries.toList(),
                optionLabel = { it.label },
                onSelected = { onSistemaEnsinoChange(it) },
                modifier = Modifier.fillMaxWidth(),
                errorMessage = uiState.validationErrors["sistema_ensino"]
            )

            ConfidenceIndicator(confidence = null)
            EnumDropdown(
                label = "Estado de conservacao",
                selectedValue = uiState.draft.estadoConservacao,
                options = EstadoConservacao.entries.toList(),
                optionLabel = { it.label },
                onSelected = { onEstadoConservacaoChange(it) },
                modifier = Modifier.fillMaxWidth(),
                errorMessage = uiState.validationErrors["estado_conservacao"]
            )

            ConfidenceIndicator(confidence = uiState.confidenceByField["data_publicacao"])
            EditableField(
                value = uiState.draft.dataPublicacao,
                onValueChange = onDataPublicacaoChange,
                label = "Ano de publicacao",
                modifier = Modifier.fillMaxWidth(),
                errorMessage = uiState.validationErrors["data_publicacao"]
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRestart
                ) {
                    Text("Trocar imagem")
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showConfirmation = true },
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
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmation = false }) {
                    Text("Editar")
                }
            }
        )
    }
}
