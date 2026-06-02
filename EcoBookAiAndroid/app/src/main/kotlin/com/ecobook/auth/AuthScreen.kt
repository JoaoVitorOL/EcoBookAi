package com.ecobook.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecobook.BuildConfig
import com.ecobook.R
import com.ecobook.api.RuntimeBackendUrlOverride
import com.ecobook.model.BackendConnectionState
import com.ecobook.model.BackendStatus
import com.ecobook.ui.components.AdaptiveScreenContent
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.SectionHeading
import com.ecobook.ui.components.StatusBadge
import com.ecobook.ui.theme.backendConnectionBadgeColors
import com.ecobook.ui.theme.ecoBookAppBackgroundBrush

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
            .background(ecoBookAppBackgroundBrush())
    ) {
        AdaptiveScreenContent {
            Column(
                modifier = it
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SectionHeading(
                    title = if (uiState.mode == AuthMode.LOGIN) {
                        "Entrar no EcoBook"
                    } else {
                        "Criar conta no EcoBook"
                    },
                    subtitle = if (uiState.mode == AuthMode.LOGIN) {
                        "Entre com email e senha para continuar o cadastro do adulto responsável."
                    } else {
                        "Crie a conta do pai, mãe ou responsável legal. Os demais dados ficam para a próxima etapa do cadastro."
                    }
                )

                GlassCard {
                    Text(
                        text = "Uso destinado a pais, mães e responsáveis legais. O app não organiza conversa nem entrega dentro da plataforma: o contato e o ponto de encontro são combinados depois, via WhatsApp, entre as partes adultas.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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
                            text = uiState.errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberConfiguredBackendUrl(): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) {
        val runtimeOverride = RuntimeBackendUrlOverride.current(context)
        val override = if (!runtimeOverride.isNullOrBlank()) {
            runtimeOverride
        } else {
            BuildConfig.BACKEND_URL_OVERRIDE.trim()
        }
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
        Text(
            text = if (uiState.mode == AuthMode.LOGIN) {
                "Use o email e a senha cadastrados para retomar a sessão do responsável."
            } else {
                "Crie a conta com nome, email e senha. O WhatsApp, CPF e a localização do adulto responsável ficam para a próxima etapa."
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
                    "Ainda não tem conta? Criar conta"
                } else {
                    "Já tem conta? Entrar"
                }
            )
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val badgeColors = backendConnectionBadgeColors(backendStatus.state)

    GlassCard {
        StatusBadge(
            text = when (backendStatus.state) {
                BackendConnectionState.CHECKING -> "Verificando servidor"
                BackendConnectionState.OFFLINE -> "Servidor indisponível"
                BackendConnectionState.ONLINE -> "Servidor online"
            },
            containerColor = badgeColors.containerColor,
            contentColor = badgeColors.contentColor
        )
        Text(
            text = when (backendStatus.state) {
                BackendConnectionState.CHECKING -> "Estamos verificando a conexão com o backend local."
                BackendConnectionState.OFFLINE -> "Não foi possível falar com o backend agora. Enquanto a API estiver offline, entrar e criar conta não vão concluir."
                BackendConnectionState.ONLINE -> backendStatus.detail
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        backendOfflineHint(
            backendStatus = backendStatus,
            configuredBackendUrl = configuredBackendUrl,
            emulatorUrl = context.getString(R.string.backend_url_emulator),
            physicalUrl = context.getString(R.string.backend_url_physical)
        )?.let { hint ->
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

private fun backendOfflineHint(
    backendStatus: BackendStatus,
    configuredBackendUrl: String,
    emulatorUrl: String,
    physicalUrl: String
): String? {
    if (backendStatus.state != BackendConnectionState.OFFLINE) {
        return null
    }

    return when {
        configuredBackendUrl.contains("10.0.2.2") -> {
            "Dica: $emulatorUrl funciona no emulador Android. Se o backend estiver no WSL ou se voce estiver usando um celular fisico, troque para o IP real da maquina, por exemplo $physicalUrl."
        }

        configuredBackendUrl.contains("192.168.0.10") -> {
            "Dica: $physicalUrl e apenas um exemplo. Substitua esse endpoint pelo IP real da maquina host na mesma rede Wi-Fi."
        }

        else -> null
    }
}
