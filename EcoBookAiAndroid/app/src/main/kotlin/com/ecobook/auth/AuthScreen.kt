package com.ecobook.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecobook.BuildConfig
import com.ecobook.R
import com.ecobook.model.BackendConnectionState
import com.ecobook.model.BackendStatus
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.SectionHeading
import com.ecobook.ui.components.StatusBadge

@Composable
fun AuthScreen(
    sessionMessage: String?,
    backendStatus: BackendStatus,
    onRefreshBackend: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuredBackendUrl = rememberConfiguredBackendUrl()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5E8D0),
                        Color(0xFFF8F4ED),
                        Color(0xFFDDEBDD)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SectionHeading(
                title = "Entrar no EcoBook",
                subtitle = "Entre com email e senha ou crie sua conta para continuar o onboarding no aplicativo."
            )

            if (!sessionMessage.isNullOrBlank()) {
                GlassCard {
                    Text(
                        text = sessionMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            AuthCard(
                uiState = uiState,
                onModeSelected = viewModel::setMode,
                onNomeChange = viewModel::updateNome,
                onEmailChange = viewModel::updateEmail,
                onPasswordChange = viewModel::updatePassword,
                onConfirmPasswordChange = viewModel::updateConfirmPassword,
                onSubmit = viewModel::submit
            )

            if (backendStatus.state != BackendConnectionState.ONLINE) {
                BackendStatusCard(
                    backendStatus = backendStatus,
                    configuredBackendUrl = configuredBackendUrl,
                    onRefreshBackend = onRefreshBackend
                )
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                GlassCard {
                    Text(
                        text = uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberConfiguredBackendUrl(): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) {
        val override = BuildConfig.BACKEND_URL_OVERRIDE.trim()
        if (override.isNotBlank()) {
            override.trimEnd('/')
        } else {
            context.getString(R.string.backend_url).trimEnd('/')
        }
    }
}

@Composable
private fun AuthCard(
    uiState: AuthUiState,
    onModeSelected: (AuthMode) -> Unit,
    onNomeChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    GlassCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AuthModeButton(
                text = "Entrar",
                selected = uiState.mode == AuthMode.LOGIN,
                onClick = { onModeSelected(AuthMode.LOGIN) },
                modifier = Modifier.fillMaxWidth()
            )
            AuthModeButton(
                text = "Criar conta",
                selected = uiState.mode == AuthMode.REGISTER,
                onClick = { onModeSelected(AuthMode.REGISTER) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text(
            text = if (uiState.mode == AuthMode.LOGIN) {
                "Use o email e a senha cadastrados para retomar sua sessao."
            } else {
                "Crie sua conta com nome, email e senha. Os demais dados ficam para o onboarding."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (uiState.mode == AuthMode.REGISTER) {
            OutlinedTextField(
                value = uiState.nome,
                onValueChange = onNomeChange,
                label = { Text("Nome completo") },
                isError = uiState.fieldErrors.containsKey("nome"),
                supportingText = {
                    uiState.fieldErrors["nome"]?.let { message -> Text(message) }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            isError = uiState.fieldErrors.containsKey("email"),
            supportingText = {
                uiState.fieldErrors["email"]?.let { message -> Text(message) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        PasswordField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = "Senha",
            error = uiState.fieldErrors["password"]
        )

        if (uiState.mode == AuthMode.REGISTER) {
            PasswordField(
                value = uiState.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirmar senha",
                error = uiState.fieldErrors["confirm_password"]
            )
        }

        Button(
            onClick = onSubmit,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(if (uiState.mode == AuthMode.LOGIN) "Entrar" else "Criar conta")
            }
        }

        TextButton(
            onClick = {
                onModeSelected(
                    if (uiState.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN
                )
            },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (uiState.mode == AuthMode.LOGIN) {
                    "Ainda nao tem conta? Criar conta"
                } else {
                    "Ja tem conta? Entrar"
                }
            )
        }
    }
}

@Composable
private fun AuthModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White.copy(alpha = 0.96f),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(text)
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = !error.isNullOrBlank(),
        supportingText = {
            error?.let { Text(it) }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BackendStatusCard(
    backendStatus: BackendStatus,
    configuredBackendUrl: String,
    onRefreshBackend: () -> Unit
) {
    GlassCard {
        val (badgeLabel, badgeContainer, badgeContent) = when (backendStatus.state) {
            BackendConnectionState.CHECKING -> Triple("Verificando servidor", Color(0xFFFCE7D8), Color(0xFF8A4C1F))
            BackendConnectionState.OFFLINE -> Triple("Servidor indisponivel", Color(0xFFF7DDDB), Color(0xFF8D3D30))
            BackendConnectionState.ONLINE -> Triple("Servidor online", Color(0xFFE0EFE4), Color(0xFF205447))
        }

        StatusBadge(
            text = badgeLabel,
            containerColor = badgeContainer,
            contentColor = badgeContent
        )
        Text(
            text = when (backendStatus.state) {
                BackendConnectionState.CHECKING -> "Estamos verificando a conexao com o backend local."
                BackendConnectionState.OFFLINE -> "Nao foi possivel falar com o backend agora. O login continua disponivel e sera concluido quando a API responder."
                BackendConnectionState.ONLINE -> backendStatus.detail
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Endpoint atual: $configuredBackendUrl",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = onRefreshBackend,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Verificar backend novamente")
        }
    }
}
