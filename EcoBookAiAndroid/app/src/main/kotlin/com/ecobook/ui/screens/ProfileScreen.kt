package com.ecobook.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.ecobook.dto.UserConsentStatusDTO
import com.ecobook.model.UserProfileDraft
import com.ecobook.ui.EcoBookUiState
import com.ecobook.ui.EcoBookViewModel
import com.ecobook.ui.ProfileInputRules
import com.ecobook.ui.components.AdaptiveScreenContent
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.LegalDocumentsDialog
import com.ecobook.ui.components.NotificationsEntryPointButton
import com.ecobook.ui.components.ProfileAvatar
import com.ecobook.ui.components.SectionHeading
import com.ecobook.ui.digitsOnly
import kotlinx.coroutines.launch

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
    onUploadProfilePhoto: (Context, Uri) -> Unit,
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
    val photoError = uiState.profileFieldErrors[EcoBookViewModel.PROFILE_PHOTO_FIELD_KEY]
    val hasIdentityErrors = uiState.profileFieldErrors.keys.any { key ->
        key in setOf("nome", "email", "whatsapp", "cpf")
    }
    val hasRegionErrors = uiState.profileFieldErrors.keys.any { key ->
        key in setOf("cidade", "bairro", "instituicao")
    }
    val hasProfileChanges = remember(uiState.profile, uiState.savedProfile) {
        hasEditableProfileChanges(uiState.profile, uiState.savedProfile)
    }

    var showLegalDialog by rememberSaveable { mutableStateOf(false) }
    var activeSectionKey by rememberSaveable { mutableStateOf(ProfileSection.OVERVIEW.key) }
    var expandedMenuCategories by rememberSaveable {
        mutableStateOf(setOf("conta", "configuracoes"))
    }

    val activeSection = ProfileSection.fromKey(activeSectionKey)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()
    val profilePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onUploadProfilePhoto(context, it) }
    }

    val menuCategories = remember {
        listOf(
            ProfileMenuCategory(
                key = "conta",
                title = "Conta",
                items = listOf(
                    ProfileMenuItem(
                        key = ProfileSection.OVERVIEW.key,
                        label = "Visão geral",
                        icon = Icons.Rounded.Person,
                        destination = ProfileMenuDestination.Section(ProfileSection.OVERVIEW)
                    ),
                    ProfileMenuItem(
                        key = ProfileSection.ACCOUNT.key,
                        label = "Identidade e contato",
                        icon = Icons.Rounded.Person,
                        destination = ProfileMenuDestination.Section(ProfileSection.ACCOUNT)
                    ),
                    ProfileMenuItem(
                        key = ProfileSection.REGION.key,
                        label = "Região e instituição",
                        icon = Icons.Rounded.LocationOn,
                        destination = ProfileMenuDestination.Section(ProfileSection.REGION)
                    )
                )
            ),
            ProfileMenuCategory(
                key = "personalizacao",
                title = "Personalização",
                items = listOf(
                    ProfileMenuItem(
                        key = ProfileSection.PERSONALIZATION.key,
                        label = "Foto e aparência",
                        icon = Icons.Rounded.Palette,
                        destination = ProfileMenuDestination.Section(ProfileSection.PERSONALIZATION)
                    )
                )
            ),
            ProfileMenuCategory(
                key = "configuracoes",
                title = "Configurações",
                items = listOf(
                    ProfileMenuItem(
                        key = ProfileSection.SETTINGS.key,
                        label = "Preferências do app",
                        icon = Icons.Rounded.Tune,
                        destination = ProfileMenuDestination.Section(ProfileSection.SETTINGS)
                    ),
                    ProfileMenuItem(
                        key = "legal",
                        label = "Termos e privacidade",
                        icon = Icons.Rounded.Description,
                        destination = ProfileMenuDestination.Legal
                    )
                )
            )
        )
    }

    LaunchedEffect(activeSectionKey) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(photoError, hasIdentityErrors, hasRegionErrors) {
        val targetSection = when {
            photoError != null -> ProfileSection.PERSONALIZATION
            hasIdentityErrors -> ProfileSection.ACCOUNT
            hasRegionErrors -> ProfileSection.REGION
            else -> null
        } ?: return@LaunchedEffect

        activeSectionKey = targetSection.key
        expandedMenuCategories = expandedMenuCategories + targetSection.categoryKey
    }

    fun toggleCategory(categoryKey: String) {
        expandedMenuCategories = if (categoryKey in expandedMenuCategories) {
            expandedMenuCategories - categoryKey
        } else {
            expandedMenuCategories + categoryKey
        }
    }

    fun openSection(section: ProfileSection) {
        activeSectionKey = section.key
        expandedMenuCategories = expandedMenuCategories + section.categoryKey
        scope.launch { drawerState.close() }
    }

    fun handleMenuSelection(item: ProfileMenuItem) {
        when (val destination = item.destination) {
            is ProfileMenuDestination.Section -> openSection(destination.section)
            ProfileMenuDestination.Legal -> {
                showLegalDialog = true
                scope.launch { drawerState.close() }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                ProfileDrawerHeader(
                    name = uiState.profile.nome.ifBlank { "Conta EcoBook" },
                    email = uiState.profile.email.ifBlank { "Atualize seus dados" },
                    imageUrl = uiState.profile.fotoPerfilUrl.ifBlank { null }
                )
                Divider(modifier = Modifier.padding(horizontal = 20.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    menuCategories.forEach { category ->
                        ProfileDrawerCategory(
                            category = category,
                            expanded = category.key in expandedMenuCategories,
                            activeSection = activeSection,
                            onToggleExpanded = { toggleCategory(category.key) },
                            onItemClick = ::handleMenuSelection
                        )
                    }
                }
            }
        }
    ) {
        AdaptiveScreenContent {
            LazyColumn(
                state = listState,
                modifier = it.imePadding(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    SectionHeading(
                        title = "Seu perfil",
                        subtitle = activeSection.subtitle,
                        trailingContent = {
                            NotificationsEntryPointButton(
                                unreadCount = unreadNotifications,
                                onClick = onOpenNotifications
                            )
                            FilledTonalIconButton(
                                onClick = { scope.launch { drawerState.open() } }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Menu,
                                    contentDescription = "Abrir menu do perfil"
                                )
                            }
                        }
                    )
                }

                item {
                    when (activeSection) {
                        ProfileSection.OVERVIEW -> ProfileOverviewSection(
                            uiState = uiState,
                            onUploadProfilePhoto = { profilePhotoPicker.launch("image/*") },
                            onReadLegalDocuments = { showLegalDialog = true },
                            onLogout = onLogout,
                            onOpenDeleteAccount = onOpenDeleteAccount
                        )

                        ProfileSection.ACCOUNT -> ProfileAccountSection(
                            uiState = uiState,
                            onNameChange = onNameChange,
                            onEmailChange = onEmailChange,
                            onWhatsappChange = onWhatsappChange,
                            onCpfChange = onCpfChange
                        )

                        ProfileSection.REGION -> ProfileRegionSection(
                            uiState = uiState,
                            cityPreview = cityPreview,
                            onCityChange = onCityChange,
                            onNeighborhoodChange = onNeighborhoodChange,
                            onInstitutionChange = onInstitutionChange
                        )

                        ProfileSection.PERSONALIZATION -> ProfilePersonalizationSection(
                            uiState = uiState,
                            darkModeEnabled = darkModeEnabled,
                            photoError = photoError,
                            onUploadProfilePhoto = { profilePhotoPicker.launch("image/*") },
                            onToggleDarkTheme = onToggleDarkTheme,
                            onFollowSystemTheme = onFollowSystemTheme
                        )

                        ProfileSection.SETTINGS -> ProfileSettingsSection(
                            consentimentoIa = consentimentoIa,
                            consentStatus = consentStatus,
                            uiState = uiState,
                            onReadLegalDocuments = { showLegalDialog = true },
                            onToggleAiConsent = onToggleAiConsent
                        )
                    }
                }

                if (!uiState.profileMessage.isNullOrBlank() && !hasProfileChanges) {
                    item {
                        ProfileFeedbackCard(
                            message = uiState.profileMessage,
                            isError = uiState.profileMessageIsError
                        )
                    }
                }

                if (hasProfileChanges) {
                    item {
                        ProfileSaveActionCard(
                            uiState = uiState,
                            onSaveProfile = onSaveProfile
                        )
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
private fun ProfileQuickSecurityActionsCard(
    uiState: EcoBookUiState,
    onLogout: () -> Unit,
    onOpenDeleteAccount: () -> Unit
) {
    GlassCard {
        ProfileCardHeader(
            title = "Sessão da conta",
            subtitle = "As ações de sair da conta e excluir conta ficam visíveis aqui em qualquer área do perfil."
        )
        Text(
            text = "Use sair da conta para encerrar o acesso deste aparelho ou exclusão para remover definitivamente os dados vinculados à conta.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ProfileSecurityActionButtons(
            uiState = uiState,
            onLogout = onLogout,
            onOpenDeleteAccount = onOpenDeleteAccount
        )
    }
}

@Composable
private fun ProfileOverviewSection(
    uiState: EcoBookUiState,
    onUploadProfilePhoto: () -> Unit,
    onReadLegalDocuments: () -> Unit,
    onLogout: () -> Unit,
    onOpenDeleteAccount: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileSectionHeader(
            title = "Visão geral",
            subtitle = "Uma entrada limpa da conta. Use o menu lateral para abrir apenas a seção que quiser editar."
        )
        GlassCard {
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
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = uiState.profile.nome.ifBlank { "Atualize os dados da sua conta" },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = uiState.profile.email.ifBlank { "Nenhum email sincronizado" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = buildLocationSummary(uiState),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "Esta conta é destinada a pais, mães e responsáveis legais. A retirada e a entrega são combinadas fora do app, pelo WhatsApp.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onUploadProfilePhoto,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoCamera,
                        contentDescription = null
                    )
                    Text("Trocar foto")
                }
                OutlinedButton(
                    onClick = onReadLegalDocuments,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = null
                    )
                    Text("Ver termos")
                }
            }
        }
        ProfileQuickSecurityActionsCard(
            uiState = uiState,
            onLogout = onLogout,
            onOpenDeleteAccount = onOpenDeleteAccount
        )
    }
}

@Composable
private fun ProfileAccountSection(
    uiState: EcoBookUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onWhatsappChange: (String) -> Unit,
    onCpfChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileSectionHeader(
            title = "Conta",
            subtitle = "Edite apenas seus dados principais de identificação e contato."
        )
        GlassCard {
            ProfileCardHeader(
                title = "Identidade e contato",
                subtitle = "Esses dados identificam o responsável que usará a conta."
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
                supportingText = "Ao alterar o email, o próximo login deverá ser feito com o endereço atualizado.",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            ProfileField(
                label = "WhatsApp",
                value = uiState.profile.whatsapp,
                onValueChange = onWhatsappChange,
                error = uiState.profileFieldErrors["whatsapp"],
                supportingText = "Informe os 11 dígitos com DDD. O código +55 é aplicado automaticamente no envio.",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                inputFilter = { it.digitsOnly(maxDigits = 11) }
            )
            ProfileField(
                label = "CPF do adulto responsável",
                value = uiState.profile.cpf,
                onValueChange = onCpfChange,
                error = uiState.profileFieldErrors["cpf"],
                supportingText = "Use o CPF do responsável legal que administrará a conta.",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                inputFilter = { it.digitsOnly(maxDigits = 11) }
            )
        }
    }
}

@Composable
private fun ProfileRegionSection(
    uiState: EcoBookUiState,
    cityPreview: String,
    onCityChange: (String) -> Unit,
    onNeighborhoodChange: (String) -> Unit,
    onInstitutionChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileSectionHeader(
            title = "Região e instituição",
            subtitle = "Deixe sua localização clara para facilitar a combinação entre as partes."
        )
        GlassCard {
            ProfileCardHeader(
                title = "Localização da conta",
                subtitle = "Essas informações ajudam na retirada, na entrega e na leitura do contexto do material."
            )
            ProfileField(
                label = "Cidade",
                value = uiState.profile.cidade,
                onValueChange = onCityChange,
                error = uiState.profileFieldErrors["cidade"],
                supportingText = if (cityPreview.isBlank()) {
                    "A cidade será padronizada antes de salvar."
                } else {
                    "Prévia de armazenamento: $cityPreview"
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
                supportingText = "Opcional, mas útil para contextualizar o material e a forma de retirada.",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
            Text(
                text = "O ponto de encontro e o horário da entrega são combinados fora do app, diretamente pelo WhatsApp.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfilePersonalizationSection(
    uiState: EcoBookUiState,
    darkModeEnabled: Boolean,
    photoError: String?,
    onUploadProfilePhoto: () -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onFollowSystemTheme: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileSectionHeader(
            title = "Personalização",
            subtitle = "Ajuste a aparência da conta e a imagem usada para identificação."
        )
        GlassCard {
            ProfileCardHeader(
                title = "Foto e aparência",
                subtitle = "A foto ajuda o doador e o responsável a se reconhecerem antes da entrega."
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatar(
                    imageUrl = uiState.profile.fotoPerfilUrl.ifBlank { null },
                    name = uiState.profile.nome.ifBlank { "Perfil EcoBook" },
                    modifier = Modifier.size(92.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Use uma foto nítida para facilitar a identificação no momento da retirada.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = onUploadProfilePhoto,
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
                text = photoError
                    ?: "Aceitamos JPG ou PNG com até 5 MB. Se necessário, recorte a imagem antes de enviar.",
                style = MaterialTheme.typography.bodySmall,
                color = if (photoError != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Divider()
            SettingsSwitchRow(
                icon = Icons.Rounded.DarkMode,
                title = "Tema escuro",
                supportingText = when (uiState.darkThemeOverride) {
                    null -> "No momento, o app acompanha automaticamente o tema do sistema."
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
}

@Composable
private fun ProfileSettingsSection(
    consentimentoIa: Boolean,
    consentStatus: UserConsentStatusDTO?,
    uiState: EcoBookUiState,
    onReadLegalDocuments: () -> Unit,
    onToggleAiConsent: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileSectionHeader(
            title = "Configurações",
            subtitle = "Reveja os termos da plataforma e ajuste as preferências do app."
        )
        GlassCard {
            ProfileCardHeader(
                title = "Preferências do app",
                subtitle = "Aqui ficam os termos da plataforma e as preferências opcionais desta conta."
            )
            SettingsActionRow(
                icon = Icons.Rounded.Description,
                title = "Termos e privacidade",
                supportingText = if (consentStatus?.platformConsentGiven == true) {
                    "Aceite registrado${consentStatus.platformConsentGivenAt?.let { " em ${formatDate(it)}" } ?: ""}."
                } else {
                    "Leia como o app funciona, quais dados são usados e quais são os direitos garantidos pela LGPD."
                },
                actionLabel = "Ler",
                onClick = onReadLegalDocuments
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
        }
    }
}

@Composable
private fun ProfileSecurityActionButtons(
    uiState: EcoBookUiState,
    onLogout: () -> Unit,
    onOpenDeleteAccount: () -> Unit
) {
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
            Text("Sair da conta")
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

@Composable
private fun ProfileSaveActionCard(
    uiState: EcoBookUiState,
    onSaveProfile: () -> Unit
) {
    GlassCard {
        ProfileCardHeader(
            title = "Salvar alterações",
            subtitle = "Há mudanças pendentes nos campos editáveis desta conta."
        )
        ProfileMessage(
            message = uiState.profileMessage,
            isError = uiState.profileMessageIsError
        )
        Button(
            onClick = onSaveProfile,
            enabled = !uiState.isSavingProfile &&
                !uiState.isUpdatingAiConsent &&
                !uiState.isUploadingProfilePhoto,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Rounded.Save,
                contentDescription = null
            )
            Text(if (uiState.isSavingProfile) "Salvando..." else "Salvar alterações")
        }
    }
}

@Composable
private fun ProfileFeedbackCard(
    message: String?,
    isError: Boolean
) {
    if (message.isNullOrBlank()) {
        return
    }

    GlassCard {
        ProfileMessage(
            message = message,
            isError = isError
        )
    }
}

@Composable
private fun ProfileSectionHeader(
    title: String,
    subtitle: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileDrawerHeader(
    name: String,
    email: String,
    imageUrl: String?
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(
                imageUrl = imageUrl,
                name = name,
                modifier = Modifier.size(64.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "Abra uma categoria para ver somente a seção desejada.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileDrawerCategory(
    category: ProfileMenuCategory,
    expanded: Boolean,
    activeSection: ProfileSection,
    onToggleExpanded: () -> Unit,
    onItemClick: (ProfileMenuItem) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleSmall
                )
            },
            trailingContent = {
                Icon(
                    imageVector = if (expanded) {
                        Icons.Rounded.KeyboardArrowUp
                    } else {
                        Icons.Rounded.KeyboardArrowDown
                    },
                    contentDescription = null
                )
            }
        )
        AnimatedVisibility(visible = expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                category.items.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        selected = item.destination.isSelected(activeSection),
                        onClick = { onItemClick(item) },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null
                            )
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedContainerColor = Color.Transparent
                        )
                    )
                }
            }
        }
        Divider(
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
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
    consentStatus: UserConsentStatusDTO?,
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

private fun hasEditableProfileChanges(
    current: UserProfileDraft,
    saved: UserProfileDraft
): Boolean {
    return normalizeEditableField(current.nome) != normalizeEditableField(saved.nome) ||
        normalizeEditableField(current.email) != normalizeEditableField(saved.email) ||
        normalizeEditableField(current.whatsapp) != normalizeEditableField(saved.whatsapp) ||
        normalizeEditableField(current.cpf) != normalizeEditableField(saved.cpf) ||
        normalizeEditableField(current.cidade) != normalizeEditableField(saved.cidade) ||
        normalizeEditableField(current.bairro) != normalizeEditableField(saved.bairro) ||
        normalizeEditableField(current.instituicao) != normalizeEditableField(saved.instituicao)
}

private fun normalizeEditableField(value: String): String = value.trim()

private fun buildLocationSummary(uiState: EcoBookUiState): String {
    val location = listOf(
        uiState.profile.cidade.trim(),
        uiState.profile.bairro.trim()
    ).filter(String::isNotBlank)
        .joinToString(" • ")

    val institution = uiState.profile.instituicao.trim()
    return when {
        location.isNotBlank() && institution.isNotBlank() -> "$location • $institution"
        location.isNotBlank() -> location
        institution.isNotBlank() -> institution
        else -> "Adicione sua cidade, bairro e instituição para completar a conta."
    }
}

private fun formatDate(rawValue: String): String {
    val normalized = rawValue.trim()
    if (normalized.length < 10) {
        return normalized
    }
    return normalized.substring(0, 10)
}

private enum class ProfileSection(
    val key: String,
    val subtitle: String,
    val categoryKey: String
) {
    OVERVIEW(
        key = "overview",
        subtitle = "Abra uma categoria no menu lateral para ver apenas a área que deseja usar.",
        categoryKey = "conta"
    ),
    ACCOUNT(
        key = "account",
        subtitle = "Edite seus dados principais sem misturar tudo em uma tela só.",
        categoryKey = "conta"
    ),
    REGION(
        key = "region",
        subtitle = "Ajuste localização e instituição em uma seção separada.",
        categoryKey = "conta"
    ),
    PERSONALIZATION(
        key = "personalization",
        subtitle = "Cuide da foto e da aparência do app nesta área.",
        categoryKey = "personalizacao"
    ),
    SETTINGS(
        key = "settings",
        subtitle = "Preferências, termos e IA ficam concentrados aqui.",
        categoryKey = "configuracoes"
    );

    companion object {
        fun fromKey(key: String): ProfileSection {
            return entries.firstOrNull { section -> section.key == key } ?: OVERVIEW
        }
    }
}

private data class ProfileMenuCategory(
    val key: String,
    val title: String,
    val items: List<ProfileMenuItem>
)

private data class ProfileMenuItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val destination: ProfileMenuDestination
)

private sealed interface ProfileMenuDestination {
    data class Section(val section: ProfileSection) : ProfileMenuDestination
    data object Legal : ProfileMenuDestination

    fun isSelected(activeSection: ProfileSection): Boolean {
        return when (this) {
            is Section -> section == activeSection
            Legal -> false
        }
    }
}
