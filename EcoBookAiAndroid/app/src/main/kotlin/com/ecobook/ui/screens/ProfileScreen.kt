package com.ecobook.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ecobook.ui.EcoBookUiState
import com.ecobook.ui.ProfileInputRules
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.LegalDocumentsDialog
import com.ecobook.ui.components.SectionHeading
import com.ecobook.ui.components.StatusBadge

@Composable
fun ProfileScreen(
    uiState: EcoBookUiState,
    unreadNotifications: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onWhatsappChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onNeighborhoodChange: (String) -> Unit,
    onInstitutionChange: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onToggleAiConsent: (Boolean) -> Unit,
    onOpenDeleteAccount: () -> Unit,
    onLogout: () -> Unit
) {
    val consentimentoIa = uiState.pendingAiConsent ?: uiState.profile.consentimentoIa
    val consentStatus = uiState.consentStatus
    val cityPreview = ProfileInputRules.cityStoragePreview(uiState.profile.cidade)
    var showLegalDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SectionHeading(
                title = "Conta e perfil",
                subtitle = "Atualize seus dados, revise os consentimentos e gerencie a seguranca da conta.",
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
                    text = "JWT ativo: ${if (uiState.session.isAuthenticated) "sim" else "nao"}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Papel atual: ${uiState.profile.roleLabel}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Conclusao do perfil: ${uiState.profile.completionPercent}%",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            GlassCard {
                SectionHeading(
                    title = "Dados editaveis",
                    subtitle = "Voce pode alterar nome, email, telefone, cidade, bairro e instituicao."
                )
                ProfileField(
                    label = "Nome",
                    value = uiState.profile.nome,
                    onValueChange = onNameChange,
                    error = uiState.profileFieldErrors["nome"]
                )
                ProfileField(
                    label = "Email",
                    value = uiState.profile.email,
                    onValueChange = onEmailChange,
                    error = uiState.profileFieldErrors["email"],
                    supportingText = "Ao alterar o email, voce precisara entrar novamente com o novo endereco.",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                ProfileField(
                    label = "WhatsApp",
                    value = uiState.profile.whatsapp,
                    onValueChange = onWhatsappChange,
                    error = uiState.profileFieldErrors["whatsapp"],
                    supportingText = "Digite DDD + numero. O codigo +55 e aplicado ao salvar.",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                ProfileField(
                    label = "Cidade",
                    value = uiState.profile.cidade,
                    onValueChange = onCityChange,
                    error = uiState.profileFieldErrors["cidade"],
                    supportingText = if (cityPreview.isBlank()) {
                        "A cidade sera padronizada antes de salvar."
                    } else {
                        "Sera salva como: $cityPreview"
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                ProfileField(
                    label = "Bairro",
                    value = uiState.profile.bairro,
                    onValueChange = onNeighborhoodChange,
                    error = uiState.profileFieldErrors["bairro"],
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                ProfileField(
                    label = "Instituicao",
                    value = uiState.profile.instituicao,
                    onValueChange = onInstitutionChange,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
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
                Button(
                    onClick = onSaveProfile,
                    enabled = !uiState.isSavingProfile && !uiState.isUpdatingAiConsent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (uiState.isSavingProfile) "Salvando..." else "Salvar alteracoes")
                }
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
                        "Plataforma: aguardando sincronizacao do aceite."
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedButton(
                    onClick = { showLegalDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ler termos e privacidade")
                }
                Text(
                    text = if (consentimentoIa) {
                        "IA para classificacao: ativa${consentStatus?.aiConsentGivenAt?.let { " desde ${formatDate(it)}" } ?: ""}."
                    } else {
                        "IA para classificacao: desativada${consentStatus?.aiConsentRevokedAt?.let { " desde ${formatDate(it)}" } ?: ""}."
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
                        text = "Salvando preferencia...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            GlassCard {
                SectionHeading(
                    title = "Sessao e seguranca",
                    subtitle = "Exporte seus dados, revise a conta atual ou encerre a sessao neste dispositivo."
                )
                Text(
                    text = "A exclusao da conta cancela materiais publicados, remove imagens armazenadas e encerra sua sessao.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSavingProfile && !uiState.isUpdatingAiConsent
                ) {
                    Text("Sair da conta")
                }
                Button(
                    onClick = onOpenDeleteAccount,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF7D6D9),
                        contentColor = Color(0xFF8A2432)
                    )
                ) {
                    Text("Excluir conta")
                }
            }
        }
    }

    if (showLegalDialog) {
        LegalDocumentsDialog(
            onDismiss = { showLegalDialog = false }
        )
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String? = null,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = error != null,
        keyboardOptions = keyboardOptions,
        supportingText = when {
            error != null -> ({ Text(error) })
            supportingText != null -> ({ Text(supportingText) })
            else -> null
        }
    )
}

private fun formatDate(rawValue: String): String {
    val normalized = rawValue.trim()
    if (normalized.length < 10) {
        return normalized
    }
    return normalized.substring(0, 10)
}
