package com.ecobook.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ecobook.ui.EcoBookUiState
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.SectionHeading
import com.ecobook.ui.components.StatusBadge

@Composable
fun ProfileScreen(
    uiState: EcoBookUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onWhatsappChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onNeighborhoodChange: (String) -> Unit,
    onInstitutionChange: (String) -> Unit,
    onToggleConsent: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SectionHeading(
                title = "Perfil e onboarding",
                subtitle = "Tela preparada para espelhar os campos do backend e evoluir para GET /usuarios/me e PATCH /usuarios/{id}."
            )
        }

        item {
            GlassCard {
                StatusBadge(
                    text = "Perfil ${uiState.profile.completionPercent}% completo",
                    containerColor = androidx.compose.ui.graphics.Color(0xFFE0EFE4),
                    contentColor = androidx.compose.ui.graphics.Color(0xFF205447)
                )
                Text(
                    text = "Foco atual: deixar a experiencia do app pronta enquanto o backend fecha auth e perfil completo.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Papel atual: ${uiState.profile.roleLabel}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (uiState.profile.hasSavedSession) {
                        "Sessao local encontrada no armazenamento seguro."
                    } else {
                        "Ainda nao existe token salvo neste dispositivo."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        item {
            GlassCard {
                ProfileField(
                    label = "Nome",
                    value = uiState.profile.nome,
                    onValueChange = onNameChange
                )
                ProfileField(
                    label = "Email",
                    value = uiState.profile.email,
                    onValueChange = onEmailChange
                )
                ProfileField(
                    label = "WhatsApp",
                    value = uiState.profile.whatsapp,
                    onValueChange = onWhatsappChange
                )
                ProfileField(
                    label = "Cidade",
                    value = uiState.profile.cidade,
                    onValueChange = onCityChange
                )
                ProfileField(
                    label = "Bairro",
                    value = uiState.profile.bairro,
                    onValueChange = onNeighborhoodChange
                )
                ProfileField(
                    label = "Instituicao",
                    value = uiState.profile.instituicao,
                    onValueChange = onInstitutionChange
                )
            }
        }

        item {
            GlassCard {
                Text(
                    text = "Consentimento para IA",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Quando este campo estiver ligado, o app ja fica alinhado com a regra de consentimento_ia do backend para Gemini.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = uiState.profile.consentimentoIa,
                    onCheckedChange = { onToggleConsent() }
                )
            }
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
