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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.SectionHeading
import com.ecobook.ui.components.StatusBadge
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    sessionMessage: String?,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val googleAuthClient = rememberAuthClient()
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var autoSignInAttempted by rememberSaveable { mutableStateOf(false) }
    var showAddGoogleAccountAction by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(googleAuthClient, context, autoSignInAttempted) {
        if (autoSignInAttempted || !googleAuthClient.isConfigured()) {
            return@LaunchedEffect
        }

        autoSignInAttempted = true
        googleAuthClient.tryAutomaticSignIn(context)
            .onSuccess { idToken ->
                if (!idToken.isNullOrBlank()) {
                    viewModel.signInWithGoogleIdToken(idToken)
                }
            }
            .onFailure { error ->
                viewModel.onAuthFailure(error.message ?: "Falha ao iniciar o login com Google.")
            }
    }

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
                subtitle = "Autentique com Google para receber seu JWT, carregar o perfil atual e seguir direto para o onboarding."
            )

            GlassCard {
                StatusBadge(
                    text = "Backend protegido por JWT",
                    containerColor = Color(0xFFE0EFE4),
                    contentColor = Color(0xFF205447)
                )
                Text(
                    text = "O app agora bloqueia o fluxo principal ate existir uma sessao valida. Se a API devolver 401, a sessao local e encerrada automaticamente.",
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

            GlassCard {
                Text(
                    text = "Continuar com Google",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "O app usa o fluxo nativo do Android Credential Manager. Para funcionar, google.oauth.clientId precisa receber o Web client ID do tipo Web application.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            googleAuthClient.signIn(context)
                                .onSuccess { idToken ->
                                    showAddGoogleAccountAction = false
                                    viewModel.signInWithGoogleIdToken(idToken)
                                }
                                .onFailure { error ->
                                    showAddGoogleAccountAction = error is GoogleAuthClient.AddGoogleAccountRequiredException
                                    viewModel.onAuthFailure(error.message ?: "Falha ao iniciar o login com Google.")
                                }
                        }
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.96f),
                        contentColor = Color(0xFF202124)
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF202124)
                        )
                    } else {
                        Text("Continuar com Google")
                    }
                }
            }

            if (showAddGoogleAccountAction) {
                GlassCard {
                    Text(
                        text = "Adicionar conta ao Android",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Se a conta estiver aberta so no navegador ou no app do Google, o Credential Manager ainda pode nao enxergar essa sessao. Abra a tela de contas do Android, adicione a conta Google e depois tente novamente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            googleAuthClient.openAddGoogleAccountSettings(context)
                                .onFailure { error ->
                                    viewModel.onAuthFailure(
                                        error.message ?: "Nao foi possivel abrir a tela para adicionar conta Google."
                                    )
                                }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Adicionar conta Google")
                    }
                }
            }

            if (!googleAuthClient.isConfigured()) {
                GlassCard {
                    Text(
                        text = "Ambiente local",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enquanto google.oauth.clientId nao estiver configurado com o Web client ID do tipo Web application no local.properties, voce ainda pode validar o backend colando um Google ID token manualmente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = uiState.manualGoogleToken,
                        onValueChange = viewModel::updateManualGoogleToken,
                        label = { Text("Google ID token") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { viewModel.signInWithGoogleIdToken(uiState.manualGoogleToken) },
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Entrar com token")
                    }
                }
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
private fun rememberAuthClient(): GoogleAuthClient {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) { GoogleAuthClient(context) }
}
