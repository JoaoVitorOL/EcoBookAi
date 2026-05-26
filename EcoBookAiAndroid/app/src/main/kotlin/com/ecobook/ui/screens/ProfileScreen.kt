package com.ecobook.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.io.IOException
import com.ecobook.ui.EcoBookUiState
import com.ecobook.ui.ProfileInputRules
import com.ecobook.ui.components.AdaptiveScreenContent
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.LegalDocumentsDialog
import com.ecobook.ui.components.SectionHeading
import com.ecobook.ui.components.StatusBadge
import com.ecobook.ui.theme.EcoBookTone
import com.ecobook.ui.theme.ecoBookBadgeColors

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
    onToggleDarkTheme: (Boolean) -> Unit,
    onFollowSystemTheme: () -> Unit,
    onToggleAiConsent: (Boolean) -> Unit,
    onExportData: () -> Unit,
    onExportSaved: (String) -> Unit,
    onExportCanceled: () -> Unit,
    onExportFailed: (String) -> Unit,
    onOpenDeleteAccount: () -> Unit,
    onLogout: () -> Unit
) {
    val consentimentoIa = uiState.pendingAiConsent ?: uiState.profile.consentimentoIa
    val consentStatus = uiState.consentStatus
    val cityPreview = ProfileInputRules.cityStoragePreview(uiState.profile.cidade)
    val darkModeEnabled = uiState.darkThemeOverride ?: isSystemInDarkTheme()
    val context = LocalContext.current
    val pendingExport = uiState.pendingPersonalDataExport
    var showLegalDialog by rememberSaveable { mutableStateOf(false) }
    val profileCompletionColors = ecoBookBadgeColors(
        if (uiState.session.profileComplete) {
            EcoBookTone.Success
        } else {
            EcoBookTone.Warning
        }
    )
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        when {
            uri == null -> onExportCanceled()
            pendingExport == null -> onExportFailed("Nenhum arquivo de exportacao estava pronto para salvar.")
            else -> savePersonalDataExport(context, pendingExport, uri)
                .onSuccess { onExportSaved(pendingExport.fileName) }
                .onFailure { error ->
                    onExportFailed(
                        error.message ?: "Nao foi possivel salvar o arquivo de exportacao."
                    )
                }
        }
    }

    LaunchedEffect(pendingExport?.requestId) {
        pendingExport?.let { exportLauncher.launch(it.fileName) }
    }

    AdaptiveScreenContent {
        LazyColumn(
            modifier = it.imePadding(),
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
                        containerColor = profileCompletionColors.containerColor,
                        contentColor = profileCompletionColors.contentColor
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
                                MaterialTheme.colorScheme.primary
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
                        title = "Aparencia",
                        subtitle = "Controle como o app deve aparecer neste dispositivo."
                    )
                    Text(
                        text = when (uiState.darkThemeOverride) {
                            null -> "No momento o app segue o tema do sistema."
                            true -> "Modo escuro fixado para este dispositivo."
                            false -> "Modo claro fixado para este dispositivo."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = darkModeEnabled,
                                role = Role.Switch,
                                onValueChange = onToggleDarkTheme
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (darkModeEnabled) "Modo escuro ativo" else "Modo escuro desativado",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = darkModeEnabled,
                            onCheckedChange = null
                        )
                    }
                    if (uiState.darkThemeOverride != null) {
                        OutlinedButton(
                            onClick = onFollowSystemTheme,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Seguir tema do sistema")
                        }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = consentimentoIa,
                                enabled = !uiState.isUpdatingAiConsent,
                                role = Role.Switch,
                                onValueChange = onToggleAiConsent
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (consentimentoIa) "Consentimento de IA ativo" else "Consentimento de IA desativado",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = consentimentoIa,
                            enabled = !uiState.isUpdatingAiConsent,
                            onCheckedChange = null
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
                    if (!uiState.exportMessage.isNullOrBlank()) {
                        Text(
                            text = uiState.exportMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.exportMessageIsError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                    OutlinedButton(
                        onClick = onExportData,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isExportingData && pendingExport == null
                    ) {
                        Text(if (uiState.isExportingData) "Preparando arquivo..." else "Exportar meus dados")
                    }
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSavingProfile && !uiState.isUpdatingAiConsent && !uiState.isExportingData
                    ) {
                        Text("Sair da conta")
                    }
                    Button(
                        onClick = onOpenDeleteAccount,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isExportingData,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Excluir conta")
                    }
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

private fun savePersonalDataExport(
    context: Context,
    exportFile: com.ecobook.model.PersonalDataExportFile,
    destination: Uri
): Result<Unit> {
    return runCatching {
        context.contentResolver.openOutputStream(destination, "w")?.use { outputStream ->
            outputStream.write(exportFile.bytes)
        } ?: throw IOException("Nao foi possivel abrir o destino selecionado.")
    }
}
