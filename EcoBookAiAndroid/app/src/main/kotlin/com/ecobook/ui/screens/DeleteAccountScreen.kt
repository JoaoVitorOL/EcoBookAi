package com.ecobook.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ecobook.ui.EcoBookUiState
import com.ecobook.ui.components.AdaptiveScreenContent
import com.ecobook.ui.components.GlassCard

@Composable
fun DeleteAccountScreen(
    topPadding: PaddingValues,
    uiState: EcoBookUiState,
    onDeleteAccount: (String, String) -> Unit,
    onNavigateUp: () -> Unit
) {
    var password by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    var showConfirmationDialog by rememberSaveable { mutableStateOf(false) }

    AdaptiveScreenContent {
        Column(
            modifier = it
                .padding(topPadding)
                .imePadding()
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
                    text = "Os materiais publicados serao removidos, as solicitacoes serao encerradas, as imagens salvas serao apagadas e o acesso atual sera encerrado.",
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
                OutlinedButton(
                    onClick = onNavigateUp,
                    enabled = !uiState.isDeletingAccount,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = { showConfirmationDialog = true },
                    enabled = !uiState.isDeletingAccount && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text(if (uiState.isDeletingAccount) "Excluindo..." else "Excluir conta")
                }
            }
        }
    }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = {
                Text("Deseja realmente excluir sua conta?")
            },
            text = {
                Text("Essa acao e permanente. Seus materiais ativos, imagens e a sessao atual serao encerrados.")
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmationDialog = false }
                ) {
                    Text("Voltar")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        onDeleteAccount(password, reason)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Sim, excluir")
                }
            }
        )
    }
}
