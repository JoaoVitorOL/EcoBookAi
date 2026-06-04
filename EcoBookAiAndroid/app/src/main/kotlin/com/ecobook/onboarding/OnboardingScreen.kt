@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.ecobook.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecobook.ui.ProfileInputRules
import com.ecobook.ui.digitsOnly
import com.ecobook.ui.components.AdaptiveScreenContent
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.LegalDocumentsDialog
import com.ecobook.ui.components.SectionHeading
import com.ecobook.ui.components.StatusBadge
import com.ecobook.ui.theme.EcoBookTone
import com.ecobook.ui.theme.ecoBookAppBackgroundBrush
import com.ecobook.ui.theme.ecoBookBadgeColors

@Composable
fun OnboardingScreen(
    onLogout: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLegalDialog by rememberSaveable { mutableStateOf(false) }
    val stepColors = ecoBookBadgeColors(EcoBookTone.Success)
    val accountColors = ecoBookBadgeColors(EcoBookTone.Warning)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ecoBookAppBackgroundBrush())
    ) {
        AdaptiveScreenContent {
            Column(
                modifier = it
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SectionHeading(
                    title = "Completar cadastro",
                    subtitle = "O EcoBook deve ser usado por pais e responsáveis. Esses dados liberam upload, matching e solicitações protegidas pelo backend."
                )

                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusBadge(
                            text = "Etapa ${uiState.currentStep + 1} de 3",
                            containerColor = stepColors.containerColor,
                            contentColor = stepColors.contentColor
                        )
                        StatusBadge(
                            text = uiState.email.ifBlank { "Conta atual" },
                            containerColor = accountColors.containerColor,
                            contentColor = accountColors.contentColor
                        )
                    }
                    Text(
                        text = when (uiState.currentStep) {
                            0 -> "Primeiro registramos o adulto responsável e o contato que será usado fora do app."
                            1 -> "Depois registramos a região para normalização geográfica e matching."
                            else -> "Por fim, revisamos os consentimentos da plataforma e o uso opcional de IA."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onLogout,
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sair da conta")
                    }
                }

                when (uiState.currentStep) {
                    0 -> ContactStep(
                        uiState = uiState,
                        onNameChange = viewModel::updateNome,
                        onWhatsappChange = viewModel::updateWhatsapp,
                        onCpfChange = viewModel::updateCpf,
                        onWhatsappFocusLost = viewModel::onWhatsappFocusLost,
                        onCpfFocusLost = viewModel::onCpfFocusLost
                    )

                    1 -> LocationStep(
                        uiState = uiState,
                        onCityChange = viewModel::updateCidade,
                        onNeighborhoodChange = viewModel::updateBairro,
                        onInstitutionChange = viewModel::updateInstituicao
                    )

                    else -> ConsentStep(
                        uiState = uiState,
                        onOpenLegalDocuments = {
                            viewModel.markPlatformTermsViewed()
                            showLegalDialog = true
                        },
                        onTogglePlatformConsent = viewModel::togglePlatformConsentAccepted,
                        onToggleConsent = viewModel::toggleConsentimentoIa
                    )
                }

                if (!uiState.message.isNullOrBlank()) {
                    GlassCard {
                        Text(
                            text = uiState.message.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (uiState.fieldErrors.isEmpty()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.currentStep > 0) {
                        OutlinedButton(
                            onClick = viewModel::previousStep,
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Voltar")
                        }
                    }

                    Button(
                        onClick = if (uiState.isLastStep) viewModel::submit else viewModel::nextStep,
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (uiState.isLastStep) "Concluir perfil" else "Próxima etapa")
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
}

@Composable
private fun ContactStep(
    uiState: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onWhatsappChange: (String) -> Unit,
    onCpfChange: (String) -> Unit,
    onWhatsappFocusLost: () -> Unit,
    onCpfFocusLost: () -> Unit
) {
    val fieldRequesters = remember {
        mapOf(
            "nome" to BringIntoViewRequester(),
            "whatsapp" to BringIntoViewRequester(),
            "cpf" to BringIntoViewRequester()
        )
    }
    val firstInvalidField = remember(uiState.fieldErrors) {
        listOf("nome", "whatsapp", "cpf").firstOrNull(uiState.fieldErrors::containsKey)
    }

    LaunchedEffect(firstInvalidField) {
        firstInvalidField?.let { field ->
            fieldRequesters[field]?.bringIntoView()
        }
    }

    GlassCard {
        Text(
            text = "A conta precisa representar um adulto responsável pelo contato e retirada.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = uiState.nome,
            onValueChange = onNameChange,
            label = { Text("Nome completo") },
            isError = uiState.fieldErrors.containsKey("nome"),
            supportingText = uiState.fieldErrors["nome"]?.let { message -> { Text(message) } },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(fieldRequesters.getValue("nome"))
        )
        DigitOnlyTextField(
            value = uiState.whatsapp,
            onValueChange = onWhatsappChange,
            label = { Text("WhatsApp com DDD") },
            isError = uiState.fieldErrors.containsKey("whatsapp"),
            placeholder = { Text("11999999999") },
            supportingText = {
                Text(
                    uiState.fieldErrors["whatsapp"]
                        ?: "Digite apenas os 11 números do WhatsApp com DDD. O +55 será adicionado automaticamente."
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            maxDigits = 11,
            modifier = Modifier
                .bringIntoViewRequester(fieldRequesters.getValue("whatsapp"))
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (!state.isFocused) {
                        onWhatsappFocusLost()
                    }
                }
        )
        DigitOnlyTextField(
            value = uiState.cpf,
            onValueChange = onCpfChange,
            label = { Text("CPF do adulto responsável") },
            isError = uiState.fieldErrors.containsKey("cpf"),
            supportingText = {
                Text(
                    uiState.fieldErrors["cpf"]
                        ?: "Digite apenas os 11 números do CPF do adulto responsável."
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            maxDigits = 11,
            modifier = Modifier
                .bringIntoViewRequester(fieldRequesters.getValue("cpf"))
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (!state.isFocused) {
                        onCpfFocusLost()
                    }
                }
        )
    }
}

@Composable
private fun DigitOnlyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    placeholder: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    maxDigits: Int
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
            val nextValue = updatedValue.digitsOnly(maxDigits)
            textFieldValue = nextValue
            if (nextValue.text != value) {
                onValueChange(nextValue.text)
            }
        },
        label = label,
        isError = isError,
        placeholder = placeholder,
        supportingText = supportingText,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        modifier = modifier
    )
}

@Composable
private fun LocationStep(
    uiState: OnboardingUiState,
    onCityChange: (String) -> Unit,
    onNeighborhoodChange: (String) -> Unit,
    onInstitutionChange: (String) -> Unit
) {
    val cityPreview = ProfileInputRules.cityStoragePreview(uiState.cidade)
    val fieldRequesters = remember {
        mapOf(
            "cidade" to BringIntoViewRequester(),
            "bairro" to BringIntoViewRequester()
        )
    }
    val firstInvalidField = remember(uiState.fieldErrors) {
        listOf("cidade", "bairro").firstOrNull(uiState.fieldErrors::containsKey)
    }

    LaunchedEffect(firstInvalidField) {
        firstInvalidField?.let { field ->
            fieldRequesters[field]?.bringIntoView()
        }
    }

    GlassCard {
        Text(
            text = "A entrega não é organizada pelo app. O ponto de encontro é combinado depois, diretamente pelo WhatsApp entre as partes adultas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = uiState.cidade,
            onValueChange = onCityChange,
            label = { Text("Cidade") },
            isError = uiState.fieldErrors.containsKey("cidade"),
            supportingText = {
                Text(
                    uiState.fieldErrors["cidade"] ?: if (cityPreview.isBlank()) {
                        "Digite manualmente. A cidade será padronizada antes de salvar o perfil."
                    } else {
                        "Será salva como: $cityPreview"
                    }
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(fieldRequesters.getValue("cidade"))
        )
        OutlinedTextField(
            value = uiState.bairro,
            onValueChange = onNeighborhoodChange,
            label = { Text("Bairro") },
            isError = uiState.fieldErrors.containsKey("bairro"),
            supportingText = uiState.fieldErrors["bairro"]?.let { message -> { Text(message) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(fieldRequesters.getValue("bairro"))
        )
        OutlinedTextField(
            value = uiState.instituicao,
            onValueChange = onInstitutionChange,
            label = { Text("Instituição (opcional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ConsentStep(
    uiState: OnboardingUiState,
    onOpenLegalDocuments: () -> Unit,
    onTogglePlatformConsent: () -> Unit,
    onToggleConsent: () -> Unit
) {
    val platformConsentRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(uiState.fieldErrors["platform_consent"]) {
        if (uiState.fieldErrors.containsKey("platform_consent")) {
            platformConsentRequester.bringIntoView()
        }
    }

    GlassCard(
        modifier = Modifier.bringIntoViewRequester(platformConsentRequester)
    ) {
        Text(
            text = "Consentimento da plataforma",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Antes do aceite, leia o resumo dos termos. Ele deixa claro que o app é para adultos responsáveis e que a entrega é combinada fora da plataforma, via WhatsApp.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = onOpenLegalDocuments,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ler termos e privacidade")
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = uiState.platformConsentAccepted,
                    enabled = uiState.hasViewedPlatformTerms,
                    role = Role.Checkbox,
                    onValueChange = { onTogglePlatformConsent() }
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Aceito os termos e a política de privacidade",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Checkbox(
                checked = uiState.platformConsentAccepted,
                enabled = uiState.hasViewedPlatformTerms,
                onCheckedChange = null
            )
        }
        if (!uiState.hasViewedPlatformTerms) {
            Text(
                text = "Abra o resumo acima para liberar o aceite.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        uiState.fieldErrors["platform_consent"]?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    GlassCard {
        Text(
            text = "Consentimento para IA",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Quando ativado, o backend poderá usar Gemini nas futuras etapas de classificação assistida de materiais.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = uiState.consentimentoIa,
                    role = Role.Switch,
                    onValueChange = { onToggleConsent() }
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (uiState.consentimentoIa) "Consentimento de IA ativo" else "Consentimento de IA desativado",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = uiState.consentimentoIa,
                onCheckedChange = null
            )
        }
    }
}
