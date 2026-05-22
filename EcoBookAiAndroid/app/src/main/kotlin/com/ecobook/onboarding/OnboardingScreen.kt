package com.ecobook.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecobook.model.NecessidadeAcademica
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.SectionHeading
import com.ecobook.ui.components.StatusBadge

@Composable
fun OnboardingScreen(
    onLogout: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF7E6CF),
                        Color(0xFFF9F5EE),
                        Color(0xFFE3F0E3)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SectionHeading(
                title = "Completar onboarding",
                subtitle = "Esses dados desbloqueiam upload, matching e solicitações protegidas pelo backend."
            )

            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusBadge(
                        text = "Etapa ${uiState.currentStep + 1} de 3",
                        containerColor = Color(0xFFE0EFE4),
                        contentColor = Color(0xFF205447)
                    )
                    StatusBadge(
                        text = uiState.email.ifBlank { "Conta atual" },
                        containerColor = Color(0xFFFCE7D8),
                        contentColor = Color(0xFF8A4C1F)
                    )
                }
                Text(
                    text = when (uiState.currentStep) {
                        0 -> "Primeiro confirmamos quem você é e como entrar em contato."
                        1 -> "Depois registramos a região para normalização geográfica e matching."
                        else -> "Por fim, configuramos preferências acadêmicas e consentimento para IA."
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
                    onWhatsappFocusLost = viewModel::onWhatsappFocusLost
                )

                1 -> LocationStep(
                    uiState = uiState,
                    onCityChange = viewModel::updateCidade,
                    onNeighborhoodChange = viewModel::updateBairro,
                    onInstitutionChange = viewModel::updateInstituicao
                )

                else -> NeedsStep(
                    uiState = uiState,
                    onToggleNeed = viewModel::toggleNecessidade,
                    onToggleConsent = viewModel::toggleConsentimentoIa
                )
            }

            if (!uiState.message.isNullOrBlank()) {
                GlassCard {
                    Text(
                        text = uiState.message ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (uiState.fieldErrors.isEmpty()) {
                            Color(0xFF205447)
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
                    Button(
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
}

@Composable
private fun ContactStep(
    uiState: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onWhatsappChange: (String) -> Unit,
    onWhatsappFocusLost: () -> Unit
) {
    GlassCard {
        OutlinedTextField(
            value = uiState.nome,
            onValueChange = onNameChange,
            label = { Text("Nome completo") },
            isError = uiState.fieldErrors.containsKey("nome"),
            supportingText = uiState.fieldErrors["nome"]?.let { message -> { Text(message) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.whatsapp,
            onValueChange = onWhatsappChange,
            label = { Text("WhatsApp com DDD") },
            isError = uiState.fieldErrors.containsKey("whatsapp"),
            placeholder = { Text("(48) 99999-9999") },
            prefix = { Text("+55 ") },
            supportingText = {
                Text(uiState.fieldErrors["whatsapp"] ?: "Digite só DDD + número. O código do Brasil já é adicionado automaticamente.")
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (!state.isFocused) {
                        onWhatsappFocusLost()
                    }
                }
        )
    }
}

@Composable
private fun LocationStep(
    uiState: OnboardingUiState,
    onCityChange: (String) -> Unit,
    onNeighborhoodChange: (String) -> Unit,
    onInstitutionChange: (String) -> Unit
) {
    GlassCard {
        OutlinedTextField(
            value = uiState.cidade,
            onValueChange = onCityChange,
            label = { Text("Cidade") },
            isError = uiState.fieldErrors.containsKey("cidade"),
            supportingText = {
                Text(uiState.fieldErrors["cidade"] ?: "Digite manualmente. A cidade é padronizada sem acento antes de salvar o perfil.")
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.bairro,
            onValueChange = onNeighborhoodChange,
            label = { Text("Bairro") },
            isError = uiState.fieldErrors.containsKey("bairro"),
            supportingText = uiState.fieldErrors["bairro"]?.let { message -> { Text(message) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth()
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
private fun NeedsStep(
    uiState: OnboardingUiState,
    onToggleNeed: (NecessidadeAcademica) -> Unit,
    onToggleConsent: () -> Unit
) {
    GlassCard {
        Text(
            text = "Necessidades acadêmicas",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Essas preferências ajudam o matching a priorizar materiais relevantes quando as fases seguintes forem conectadas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NecessidadeAcademica.entries.forEach { necessidade ->
                FilterChip(
                    selected = uiState.necessidadesAcademicas.contains(necessidade),
                    onClick = { onToggleNeed(necessidade) },
                    label = { Text(necessidade.label) }
                )
            }
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
        Switch(
            checked = uiState.consentimentoIa,
            onCheckedChange = { onToggleConsent() }
        )
    }
}
