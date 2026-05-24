package com.ecobook.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ecobook.dto.MaterialDTO
import com.ecobook.ui.components.StatusBadge
import com.ecobook.ui.theme.EcoBookTone
import com.ecobook.ui.theme.ecoBookBadgeColors

@Composable
fun MaterialDetailDialog(
    material: MaterialDTO,
    isRequestInFlight: Boolean,
    onDismiss: () -> Unit,
    onRequestMaterial: () -> Unit
) {
    val disciplineColors = ecoBookBadgeColors(EcoBookTone.Success)
    val levelColors = ecoBookBadgeColors(EcoBookTone.Warning)
    val yearColors = ecoBookBadgeColors(EcoBookTone.Accent)
    val conditionColors = ecoBookBadgeColors(EcoBookTone.Neutral)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Fechar")
                }
                Button(
                    onClick = onRequestMaterial,
                    enabled = !isRequestInFlight
                ) {
                    Text(if (isRequestInFlight) "Enviando..." else "Solicitar material")
                }
            }
        },
        title = {
            Text(
                text = material.titulo,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
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
                        Column(
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
                Text(
                    text = "Descrição",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = material.descricao.ifBlank { "Sem descrição adicional." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        text = formatDisciplina(material.disciplina),
                        containerColor = disciplineColors.containerColor,
                        contentColor = disciplineColors.contentColor
                    )
                    StatusBadge(
                        text = formatNivelEnsino(material.nivelEnsino),
                        containerColor = levelColors.containerColor,
                        contentColor = levelColors.contentColor
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        text = formatAnoEscolar(material.ano),
                        containerColor = yearColors.containerColor,
                        contentColor = yearColors.contentColor
                    )
                    StatusBadge(
                        text = formatEstadoConservacao(material.estadoConservacao),
                        containerColor = conditionColors.containerColor,
                        contentColor = conditionColors.contentColor
                    )
                }
                MetadataLine("Sistema de ensino", formatSistemaEnsino(material.sistemaEnsino))
                MetadataLine("Local", "${material.bairro}, ${material.cidade}")
                MetadataLine("Ano de publicação", material.dataPublicacao?.toString() ?: "Não informado")
                MetadataLine("Autor", material.autor ?: "Não informado")
                MetadataLine("Editora", material.editora ?: "Não informada")
                MetadataLine("Doador", material.doador.nome)
                Text(
                    text = "O contato do doador só aparece depois que a solicitação for aprovada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                material.criadoEm?.let { criadoEm ->
                    MetadataLine(
                        "Item cadastrado em",
                        formatAbsoluteDateTime(criadoEm) ?: formatRelativeDate(criadoEm) ?: criadoEm
                    )
                }
            }
        }
    )
}

@Composable
private fun MetadataLine(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color.Transparent)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
