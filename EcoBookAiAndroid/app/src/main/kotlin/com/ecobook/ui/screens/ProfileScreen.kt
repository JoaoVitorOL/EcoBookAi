package com.ecobook.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
    onLogout: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SectionHeading(
                title = "Conta e perfil",
                subtitle = "Resumo do usuario autenticado e dos dados enviados no onboarding."
            )
        }

        item {
            GlassCard {
                StatusBadge(
                    text = if (uiState.session.profileComplete) "Perfil completo" else "Onboarding pendente",
                    containerColor = if (uiState.session.profileComplete) {
                        androidx.compose.ui.graphics.Color(0xFFE0EFE4)
                    } else {
                        androidx.compose.ui.graphics.Color(0xFFFCE7D8)
                    },
                    contentColor = if (uiState.session.profileComplete) {
                        androidx.compose.ui.graphics.Color(0xFF205447)
                    } else {
                        androidx.compose.ui.graphics.Color(0xFF8A4C1F)
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
                ReadOnlyProfileField(label = "Nome", value = uiState.profile.nome)
                ReadOnlyProfileField(label = "Email", value = uiState.profile.email)
                ReadOnlyProfileField(label = "WhatsApp", value = uiState.profile.whatsapp)
                ReadOnlyProfileField(label = "Cidade", value = uiState.profile.cidade)
                ReadOnlyProfileField(label = "Bairro", value = uiState.profile.bairro)
                ReadOnlyProfileField(label = "Instituicao", value = uiState.profile.instituicao)
            }
        }

        item {
            GlassCard {
                SectionHeading(
                    title = "Sessao e seguranca",
                    subtitle = "Use esta opcao para encerrar a sessao atual e entrar com outra conta neste dispositivo."
                )
                Text(
                    text = "Consentimento para IA: ${if (uiState.profile.consentimentoIa) "ativo" else "desativado"}",
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
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
        value = value.ifBlank { "Nao informado" },
        onValueChange = {},
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        readOnly = true
    )
}
