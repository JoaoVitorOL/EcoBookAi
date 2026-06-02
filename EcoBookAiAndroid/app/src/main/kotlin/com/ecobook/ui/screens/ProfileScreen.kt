package com.ecobook.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ecobook.ui.EcoBookUiState
import com.ecobook.ui.EcoBookViewModel
import com.ecobook.ui.ProfileInputRules
import com.ecobook.ui.components.AdaptiveScreenContent
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.LegalDocumentsDialog
import com.ecobook.ui.components.NotificationsEntryPointButton
import com.ecobook.ui.components.ProfileAvatar
import com.ecobook.ui.components.SectionHeading
import com.ecobook.ui.components.StatusBadge
import com.ecobook.ui.digitsOnly
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
    onCpfChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onNeighborhoodChange: (String) -> Unit,
    onInstitutionChange: (String) -> Unit,
    onUploadProfilePhoto: (android.content.Context, android.net.Uri) -> Unit,
    onSaveProfile: () -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onFollowSystemTheme: () -> Unit,
    onToggleAiConsent: (Boolean) -> Unit,
    onOpenDeleteAccount: () -> Unit,
    onLogout: () -> Unit
) {
    val consentimentoIa = uiState.pendingAiConsent ?: uiState.profile.consentimentoIa
    val consentStatus = uiState.consentStatus
    val cityPreview = ProfileInputRules.cityStoragePreview(uiState.profile.cidade)
    val darkModeEnabled = uiState.darkThemeOverride ?: isSystemInDarkTheme()
    var showLegalDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val editableSectionIndex = 2
    val profilePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onUploadProfilePhoto(context, it) }
    }
    val photoError = uiState.profileFieldErrors[EcoBookViewModel.PROFILE_PHOTO_FIELD_KEY]
    val profileCompletionColors = ecoBookBadgeColors(
        if (uiState.session.profileComplete) EcoBookTone.Success else EcoBookTone.Warning
    )
    val hasEditableProfileErrors = uiState.profileFieldErrors.keys.any { key ->
        key in setOf(
            EcoBookViewModel.PROFILE_PHOTO_FIELD_KEY,
            "nome",
            "email",
            "whatsapp",
            "cpf",
            "cidade",
            "bairro",
            "instituicao"
        )
    }

    LaunchedEffect(photoError, hasEditableProfileErrors) {
        if (hasEditableProfileErrors) {
            listState.animateScrollToItem(editableSectionIndex)
        }
    }

    AdaptiveScreenContent {
        LazyColumn(
            state = listState,
            modifier = it.imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                SectionHeading(
                    title = "Conta e perfil",
                    subtitle = "Gerencie os dados do adulto responsável, os consentimentos e as configurações desta conta.",
                    trailingContent = {
                        NotificationsEntryPointButton(
                            unreadCount = unreadNotifications,
                            onClick = onOpenNotifications
                        )
                    }
                )
            }

            item {
                ProfileSummaryCard(
                    uiState = uiState,
                    statusContainerColor = profileCompletionColors.containerColor,
                    statusContentColor = profileCompletionColors.contentColor
                )
            }

            item {
                GlassCard {
                    ProfileCardHeader(
                        title = "Identidade e contato",
                        subtitle = "Campos principais da conta, foto pública do perfil e contato do adulto responsável."
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileAvatar(
                            imageUrl = uiState.profile.fotoPerfilUrl.ifBlank { null },
                            name = uiState.profile.nome.ifBlank { "Perfil EcoBook" },
                            modifier = Modifier.size(88.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "A foto ajuda o doador e o responsável a se reconhecerem antes da entrega.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FilledTonalButton(
                                onClick = { profilePhotoPicker.launch("image/*") },
                                enabled = !uiState.isUploadingProfilePhoto && !uiState.isSavingProfile,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoCamera,
                                    contentDescription = null
                                )
                                Text(
                                    text = if (uiState.isUploadingProfilePhoto) {
                                        "Enviando foto..."
                                    } else {
                                        "Alterar foto de perfil"
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        text = photoError ?: "Use JPG ou PNG com até 5MB. Se necessário, recorte a imagem antes de enviar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (photoError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
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
                        supportingText = "Alterar o email exige novo login com o endereço atualizado.",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    ProfileField(
                        label = "WhatsApp",
                        value = uiState.profile.whatsapp,
                        onValueChange = onWhatsappChange,
                        error = uiState.profileFieldErrors["whatsapp"],
                        supportingText = "Informe os 11 dígitos com DDD. O +55 é aplicado no envio.",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        inputFilter = { it.digitsOnly(maxDigits = 11) }
                    )
                    ProfileField(
                        label = "CPF do adulto responsável",
                        value = uiState.profile.cpf,
                        onValueChange = onCpfChange,
                        error = uiState.profileFieldErrors["cpf"],
                        supportingText = "Use o CPF do adulto responsável pela conta.",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        inputFilter = { it.digitsOnly(maxDigits = 11) }
                    )
                }
            }

            item {
                GlassCard {
                    ProfileCardHeader(
                        title = "Região e instituição",
                        subtitle = "Esses dados organizam o perfil e alimentam a parte geográfica do matching."
                    )
                    ProfileField(
                        label = "Cidade",
                        value = uiState.profile.cidade,
                        onValueChange = onCityChange,
                        error = uiState.profileFieldErrors["cidade"],
                        supportingText = if (cityPreview.isBlank()) {
                            "A cidade será padronizada antes de salvar."
                        } else {
                            "Previsão de armazenamento: $cityPreview"
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
                        label = "Instituição",
                        value = uiState.profile.instituicao,
                        onValueChange = onInstitutionChange,
                        error = uiState.profileFieldErrors["instituicao"],
                        supportingText = "Opcional, mas útil para contextualizar o material e a retirada.",
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )
                    Text(
                        text = "O contato e o ponto de encontro são combinados fora do app, diretamente pelo WhatsApp entre as partes adultas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                GlassCard {
                    ProfileCardHeader(
                        title = "Salvar alterações",
                        subtitle = "Revise os dados do responsável antes de salvar as alterações desta conta."
                    )
                    ProfileMessage(
                        message = uiState.profileMessage,
                        isError = uiState.profileMessageIsError
                    )
                    Button(
                        onClick = onSaveProfile,
                        enabled = !uiState.isSavingProfile && !uiState.isUpdatingAiConsent && !uiState.isUploadingProfilePhoto,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.isSavingProfile) "Salvando..." else "Salvar alterações")
                    }
                }
            }

            item {
                GlassCard {
                    ProfileCardHeader(
                        title = "Preferências e consentimentos",
                        subtitle = "Padrões de exibição do app, aceite da plataforma e uso opcional de IA."
                    )
                    SettingsActionRow(
                        icon = Icons.Rounded.Description,
                        title = "Termos e privacidade",
                        supportingText = if (consentStatus?.platformConsentGiven == true) {
                            "Aceite registrado${consentStatus.platformConsentGivenAt?.let { " em ${formatDate(it)}" } ?: ""}."
                        } else {
                            "Aceite aguardando sincronização com o backend."
                        },
                        actionLabel = "Ler",
                        onClick = { showLegalDialog = true }
                    )
                    Divider()
                    SettingsSwitchRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Classificação assistida por IA",
                        supportingText = buildAiConsentSummary(
                            consentimentoIa = consentimentoIa,
                            consentStatus = consentStatus,
                            isLoading = uiState.isLoadingConsentStatus,
                            isUpdating = uiState.isUpdatingAiConsent
                        ),
                        checked = consentimentoIa,
                        enabled = !uiState.isUpdatingAiConsent,
                        onCheckedChange = onToggleAiConsent
                    )
                    Divider()
                    SettingsSwitchRow(
                        icon = Icons.Rounded.DarkMode,
                        title = "Tema escuro",
                        supportingText = when (uiState.darkThemeOverride) {
                            null -> "No momento, o app segue automaticamente o tema do sistema."
                            true -> "Tema escuro fixado para este dispositivo."
                            false -> "Tema claro fixado para este dispositivo."
                        },
                        checked = darkModeEnabled,
                        onCheckedChange = onToggleDarkTheme
                    )
                    if (uiState.darkThemeOverride != null) {
                        OutlinedButton(
                            onClick = onFollowSystemTheme,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Voltar a seguir o tema do sistema")
                        }
                    }
                }
            }

            item {
                GlassCard {
                    ProfileCardHeader(
                        title = "Privacidade e segurança",
                        subtitle = "Controle a sessão deste aparelho e os caminhos de saída da conta."
                    )
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null
                            )
                        },
                        headlineContent = {
                            Text("Uso da conta")
                        },
                        supportingContent = {
                            Text("O app foi desenhado para pais, mães e responsáveis. Materiais e imagens são removidos quando a conta é excluída.")
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isSavingProfile && !uiState.isUpdatingAiConsent
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Logout,
                                contentDescription = null
                            )
                            Text("Sair")
                        }
                        Button(
                            onClick = onOpenDeleteAccount,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null
                            )
                            Text("Excluir conta")
                        }
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
private fun ProfileSummaryCard(
    uiState: EcoBookUiState,
    statusContainerColor: Color,
    statusContentColor: Color
) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(
                imageUrl = uiState.profile.fotoPerfilUrl.ifBlank { null },
                name = uiState.profile.nome.ifBlank { "Perfil EcoBook" },
                modifier = Modifier.size(84.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = uiState.profile.nome.ifBlank { "Complete os dados da conta" },
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = uiState.profile.email.ifBlank { "Nenhum email sincronizado" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusBadge(
                    text = if (uiState.session.profileComplete) "Perfil completo" else "Cadastro em andamento",
                    containerColor = statusContainerColor,
                    contentColor = statusContentColor
                )
            }
        }
        LinearProgressIndicator(
            progress = uiState.profile.completionRatio.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Campos essenciais preenchidos: ${uiState.profile.completionPercent}%.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (uiState.session.profileComplete) {
                "A conta já está pronta para combinar aprovações, entrega e retirada pelo WhatsApp."
            } else {
                "Preencha os dados obrigatórios para liberar todos os fluxos protegidos do app."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileCardHeader(
    title: String,
    subtitle: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    supportingText: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            ),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        },
        headlineContent = {
            Text(title)
        },
        supportingContent = {
            Text(supportingText)
        },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null
            )
        }
    )
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    supportingText: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        },
        headlineContent = {
            Text(title)
        },
        supportingContent = {
            Text(supportingText)
        },
        trailingContent = {
            TextButton(onClick = onClick) {
                Text(actionLabel)
            }
        }
    )
}

@Composable
private fun ProfileMessage(
    message: String?,
    isError: Boolean
) {
    if (message.isNullOrBlank()) {
        return
    }

    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }
    )
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String? = null,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    inputFilter: ((TextFieldValue) -> TextFieldValue)? = null
) {
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }

    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = value, selection = TextRange(value.length))
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { updatedValue ->
            val nextValue = inputFilter?.invoke(updatedValue) ?: updatedValue
            textFieldValue = nextValue
            if (nextValue.text != value) {
                onValueChange(nextValue.text)
            }
        },
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

private fun buildAiConsentSummary(
    consentimentoIa: Boolean,
    consentStatus: com.ecobook.dto.UserConsentStatusDTO?,
    isLoading: Boolean,
    isUpdating: Boolean
): String {
    val baseSummary = if (consentimentoIa) {
        "Ativa${consentStatus?.aiConsentGivenAt?.let { " desde ${formatDate(it)}" } ?: ""}."
    } else {
        "Desativada${consentStatus?.aiConsentRevokedAt?.let { " desde ${formatDate(it)}" } ?: ""}."
    }

    return when {
        isUpdating -> "$baseSummary Salvando preferência..."
        isLoading -> "$baseSummary Atualizando status..."
        else -> baseSummary
    }
}

private fun formatDate(rawValue: String): String {
    val normalized = rawValue.trim()
    if (normalized.length < 10) {
        return normalized
    }
    return normalized.substring(0, 10)
}
