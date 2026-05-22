package com.ecobook.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ecobook.ui.EcoBookUiState
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.SectionHeading
import com.ecobook.ui.components.StatusBadge

@Composable
fun ProfileScreen(
    uiState: EcoBookUiState,
    unreadNotifications: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onToggleAiConsent: (Boolean) -> Unit,
    onOpenDeleteAccount: () -> Unit,
    onLogout: () -> Unit
) {
    val consentimentoIa = uiState.pendingAiConsent ?: uiState.profile.consentimentoIa
    val consentStatus = uiState.consentStatus

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SectionHeading(
                title = "Conta e perfil",
                subtitle = "Resumo do usuário autenticado, consentimentos e controles de privacidade.",
                trailingContent = {
                    com.ecobook.ui.components.NotificationsEntryPointButton(
                        unreadCount = unreadNotifications,
                        onClick = onOpenNotifications
                    )
                }
            )
        }

        item {
            GlassCard {
                StatusBadge(
                    text = if (uiState.session.profileComplete) "Perfil completo" else "Onboarding pendente",
                    containerColor = if (uiState.session.profileComplete) {
                        Color(0xFFE0EFE4)
                    } else {
                        Color(0xFFFCE7D8)
                    },
                    contentColor = if (uiState.session.profileComplete) {
                        Color(0xFF205447)
                    } else {
                        Color(0xFF8A4C1F)
                    }
                )
                Text(
                    text = "JWT ativo: ${if (uiState.session.isAuthenticated) "sim" else "não"}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Papel atual: ${uiState.profile.roleLabel}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Conclusão do perfil: ${uiState.profile.completionPercent}%",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            GlassCard {
                ReadOnlyProfileField(label = "Nome", value = uiState.profile.nome)
                ReadOnlyProfileField(label = "Email", value = uiState.profile.email)
                ReadOnlyProfileField(label = "WhatsApp", value = uiState.profile.whatsapp)
                ReadOnlyProfileField(label = "Cidade", value = uiState.profile.cidade)
                ReadOnlyProfileField(label = "Bairro", value = uiState.profile.bairro)
                ReadOnlyProfileField(label = "Instituição", value = uiState.profile.instituicao)
            }
        }

        item {
            GlassCard {
                SectionHeading(
                    title = "Consentimentos",
                    subtitle = "Acompanhe o aceite da plataforma e controle o uso opcional de IA."
                )
                Text(
                    text = if (consentStatus?.platformConsentGiven == true) {
                        "Plataforma: concedido${consentStatus.platformConsentGivenAt?.let { " em ${formatDate(it)}" } ?: ""}."
                    } else {
                        "Plataforma: aguardando sincronização do aceite."
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (consentimentoIa) {
                        "IA para classificação: ativa${consentStatus?.aiConsentGivenAt?.let { " desde ${formatDate(it)}" } ?: ""}."
                    } else {
                        "IA para classificação: desativada${consentStatus?.aiConsentRevokedAt?.let { " desde ${formatDate(it)}" } ?: ""}."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (consentimentoIa) "Consentimento de IA ativo" else "Consentimento de IA desativado",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = consentimentoIa,
                        enabled = !uiState.isUpdatingAiConsent,
                        onCheckedChange = onToggleAiConsent
                    )
                }
                if (uiState.isLoadingConsentStatus) {
                    Text(
                        text = "Atualizando status de consentimento...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (uiState.isUpdatingAiConsent) {
                    Text(
                        text = "Salvando preferência...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!uiState.profileMessage.isNullOrBlank()) {
                    Text(
                        text = uiState.profileMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.profileMessageIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            Color(0xFF205447)
                        }
                    )
                }
            }
        }

        item {
            GlassCard {
                SectionHeading(
                    title = "Sessão e segurança",
                    subtitle = "Exporte seus dados, revise a conta atual ou encerre a sessão neste dispositivo."
                )
                Text(
                    text = "A exclusão da conta cancela materiais publicados, remove imagens armazenadas e encerra sua sessão.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onOpenDeleteAccount,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Excluir conta")
                }
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isUpdatingAiConsent
                ) {
                    Text("Sair da conta")
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyProfileField(
    label: String,
    value: String
) {
    OutlinedTextField(
        value = value.ifBlank { "Não informado" },
        onValueChange = {},
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        readOnly = true
    )
}

private fun formatDate(rawValue: String): String {
    val normalized = rawValue.trim()
    if (normalized.length < 10) {
        return normalized
    }
    return normalized.substring(0, 10)
}
