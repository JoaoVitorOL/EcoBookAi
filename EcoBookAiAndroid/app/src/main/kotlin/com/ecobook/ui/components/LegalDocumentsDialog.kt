package com.ecobook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp

@Composable
fun LegalDocumentsDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Termos de uso e privacidade",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Resumo do que o EcoBook faz com sua conta e seus dados antes do aceite.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LegalSection(
                        title = "Uso da plataforma",
                        body = "O EcoBook conecta doadores e estudantes para doar materiais de estudo sem venda dentro do app. O usuário deve informar dados verdadeiros, usar somente materiais que possa compartilhar e respeitar outros usuários durante o contato e a retirada."
                    )
                    LegalSection(
                        title = "Dados coletados",
                        body = "A conta usa nome, email, telefone, cidade, bairro, instituição, consentimentos, histórico de materiais, solicitações, notificações e trilhas de auditoria para operar o serviço com segurança."
                    )
                    LegalSection(
                        title = "Como os dados são usados",
                        body = "Esses dados servem para autenticação, matching geográfico, contato entre as partes, envio de notificações, prevenção de abuso e exclusão/anonimização da conta."
                    )
                    LegalSection(
                        title = "IA opcional",
                        body = "O consentimento de IA é separado. Quando ativado, imagens de materiais podem ser analisadas para sugerir classificação. Quando desativado, o usuário continua podendo preencher os dados manualmente."
                    )
                    LegalSection(
                        title = "Compartilhamento e retenção",
                        body = "Dados de contato sensíveis só devem ser exibidos quando o fluxo da solicitação exige. O usuário pode revisar, corrigir e excluir a própria conta pelo app. Ao excluir, materiais e imagens são removidos do fluxo ativo e os registros necessários ficam anonimizados."
                    )
                    LegalSection(
                        title = "Aviso importante",
                        body = "Este texto é um rascunho operacional do MVP baseado no comportamento atual do app. Antes de uma publicação real, ele ainda deve passar por revisão jurídica e de privacidade."
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Fechar")
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalSection(
    title: String,
    body: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
