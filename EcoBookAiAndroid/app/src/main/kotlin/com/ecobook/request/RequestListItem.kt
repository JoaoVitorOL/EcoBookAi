package com.ecobook.request

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ecobook.discovery.MaterialImage
import com.ecobook.discovery.formatAbsoluteDateTime
import com.ecobook.discovery.formatAnoEscolar
import com.ecobook.discovery.formatDisciplina
import com.ecobook.discovery.formatNivelEnsino
import com.ecobook.dto.SolicitacaoDTO
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.StatusBadge

@Composable
fun StudentRequestCard(
    request: SolicitacaoDTO,
    isWorking: Boolean,
    onCancel: () -> Unit,
    onContactDonor: () -> Unit,
    onReportNonReceipt: (() -> Unit)? = null,
    hasReportedNonReceipt: Boolean = false
) {
    val material = request.material
    val statusColors = requestStatusColors(request.status)
    val canCancel = request.status == "PENDENTE" || request.status == "APROVADA"
    val canContact = request.status == "APROVADA" && !request.contatoDoador.isNullOrEmpty()
    val canReportNonReceipt = request.status == "CONCLUIDA" &&
        material?.status == "DOADO" &&
        onReportNonReceipt != null

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MaterialImage(
                imageUrl = material?.imagemUrl,
                title = material?.titulo ?: "Material solicitado",
                modifier = Modifier
                    .width(88.dp)
                    .height(122.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = material?.titulo ?: "Material solicitado",
                    style = MaterialTheme.typography.titleMedium
                )
                material?.descricao?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        text = formatRequestStatus(request.status),
                        containerColor = statusColors.first,
                        contentColor = statusColors.second
                    )
                    material?.disciplina?.let { disciplina ->
                        StatusBadge(
                            text = formatDisciplina(disciplina),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                material?.let { currentMaterial ->
                    Text(
                        text = "${formatNivelEnsino(currentMaterial.nivelEnsino)} | ${formatAnoEscolar(currentMaterial.ano)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${currentMaterial.doadorNome} • ${currentMaterial.bairro}, ${currentMaterial.cidade}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                request.aprovadoEm?.let { approvedAt ->
                    Text(
                        text = "Aprovada em ${formatAbsoluteDateTime(approvedAt) ?: approvedAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                request.concluidoEm?.takeIf { request.status == "CONCLUIDA" }?.let { completedAt ->
                    Text(
                        text = "Doação concluída em ${formatAbsoluteDateTime(completedAt) ?: completedAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (request.status == "APROVADA") {
                    val expiry = request.expiresAt?.let { formatAbsoluteDateTime(it) ?: it }
                    Text(
                        text = if (expiry != null) {
                            "Contato liberado. Reserva ativa até $expiry."
                        } else {
                            "Contato liberado. Combine a retirada com o doador."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (canCancel || canContact || canReportNonReceipt || hasReportedNonReceipt) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (canCancel || canContact) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (canContact) {
                                    Button(
                                        onClick = onContactDonor,
                                        enabled = !isWorking,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Falar com doador")
                                    }
                                }
                                if (canCancel) {
                                    OutlinedButton(
                                        onClick = onCancel,
                                        enabled = !isWorking,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (isWorking) "Atualizando..." else "Cancelar")
                                    }
                                }
                            }
                        }

                        if (canReportNonReceipt || hasReportedNonReceipt) {
                            OutlinedButton(
                                onClick = { onReportNonReceipt?.invoke() },
                                enabled = !isWorking && !hasReportedNonReceipt,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    when {
                                        hasReportedNonReceipt -> "Reporte enviado"
                                        isWorking -> "Enviando reporte..."
                                        else -> "Reportar não recebimento"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DonorRequestCard(
    request: SolicitacaoDTO,
    isWorking: Boolean,
    onApprove: (() -> Unit)? = null,
    onDecline: (() -> Unit)? = null,
    onComplete: (() -> Unit)? = null,
    onRevokeApproval: (() -> Unit)? = null
) {
    val material = request.material
    val student = request.estudante
    val statusColors = requestStatusColors(request.status)

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MaterialImage(
                imageUrl = material?.imagemUrl,
                title = material?.titulo ?: "Material",
                modifier = Modifier
                    .width(88.dp)
                    .height(122.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = material?.titulo ?: "Material",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        text = formatRequestStatus(request.status),
                        containerColor = statusColors.first,
                        contentColor = statusColors.second
                    )
                    material?.disciplina?.let { disciplina ->
                        StatusBadge(
                            text = formatDisciplina(disciplina),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                student?.let { currentStudent ->
                    Text(
                        text = currentStudent.nome,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${currentStudent.bairro}, ${currentStudent.cidade}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                material?.let { currentMaterial ->
                    Text(
                        text = "${formatNivelEnsino(currentMaterial.nivelEnsino)} | ${formatAnoEscolar(currentMaterial.ano)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                request.criadoEm?.let { createdAt ->
                    Text(
                        text = "Pedido criado em ${formatAbsoluteDateTime(createdAt) ?: createdAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (request.status == "APROVADA") {
                    request.expiresAt?.let { expiry ->
                        Text(
                            text = "Reserva ativa até ${formatAbsoluteDateTime(expiry) ?: expiry}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                when {
                    onApprove != null && onDecline != null -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onDecline,
                                enabled = !isWorking,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isWorking) "Processando..." else "Recusar")
                            }
                            Button(
                                onClick = onApprove,
                                enabled = !isWorking,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isWorking) "Processando..." else "Aprovar")
                            }
                        }
                    }

                    onComplete != null -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onComplete,
                                enabled = !isWorking,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isWorking) "Processando..." else "Marcar como doado")
                            }
                            onRevokeApproval?.let { revoke ->
                                OutlinedButton(
                                    onClick = revoke,
                                    enabled = !isWorking,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (isWorking) "Processando..." else "Revogar aprovação")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
