package com.ecobook.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ecobook.ui.EcoBookUiState
import com.ecobook.ui.components.GlassCard

@Composable
fun DeleteAccountScreen(
    topPadding: PaddingValues,
    uiState: EcoBookUiState,
    onDeleteAccount: (String, String) -> Unit,
    onNavigateUp: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(topPadding)
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        GlassCard {
            Text(
                text = "Antes de excluir a conta",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Os materiais publicados serão removidos, as solicitações serão encerradas, as imagens salvas serão apagadas e o acesso atual será encerrado.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        GlassCard {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha atual") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation()
            )
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Motivo (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            if (!uiState.accountDeletionMessage.isNullOrBlank()) {
                Text(
                    text = uiState.accountDeletionMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.accountDeletionMessageIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
            Button(
                onClick = { onDeleteAccount(password, reason) },
                enabled = !uiState.isDeletingAccount && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isDeletingAccount) "Excluindo..." else "Confirmar exclusão")
            }
            OutlinedButton(
                onClick = onNavigateUp,
                enabled = !uiState.isDeletingAccount,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    }
}
