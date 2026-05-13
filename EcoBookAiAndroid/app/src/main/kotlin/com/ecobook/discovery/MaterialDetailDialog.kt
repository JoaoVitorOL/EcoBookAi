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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecobook.dto.MaterialDTO
import com.ecobook.ui.components.StatusBadge

@Composable
fun MaterialDetailDialog(
    material: MaterialDTO,
    isRequestInFlight: Boolean,
    onDismiss: () -> Unit,
    onRequestMaterial: () -> Unit
) {
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
                MaterialImage(
                    imageUrl = material.imagemUrl,
                    title = material.titulo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
                Text(
                    text = "Descricao",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = material.descricao.ifBlank { "Sem descricao adicional." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        text = formatDisciplina(material.disciplina),
                        containerColor = Color(0xFFE5F0EA),
                        contentColor = Color(0xFF205447)
                    )
                    StatusBadge(
                        text = formatNivelEnsino(material.nivelEnsino),
                        containerColor = Color(0xFFFCE7D8),
                        contentColor = Color(0xFF8A4C1F)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        text = formatAnoEscolar(material.ano),
                        containerColor = Color(0xFFF1EAF8),
                        contentColor = Color(0xFF5E427E)
                    )
                    StatusBadge(
                        text = formatEstadoConservacao(material.estadoConservacao),
                        containerColor = Color(0xFFE6EEF8),
                        contentColor = Color(0xFF214A73)
                    )
                }
                MetadataLine("Sistema de ensino", formatSistemaEnsino(material.sistemaEnsino))
                MetadataLine("Local", "${material.bairro}, ${material.cidade}")
                MetadataLine("Ano de publicacao", material.dataPublicacao?.toString() ?: "Nao informado")
                MetadataLine("Autor", material.autor ?: "Nao informado")
                MetadataLine("Editora", material.editora ?: "Nao informada")
                MetadataLine("Doador", material.doador.nome)
                Text(
                    text = "O contato do doador so aparece depois que a solicitacao for aprovada.",
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
            .background(Color.Transparent)
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
