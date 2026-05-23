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
                        body = "O EcoBook conecta doadores e estudantes para doar materiais de estudo sem venda dentro do app. O usuario deve informar dados verdadeiros, usar somente materiais que possa compartilhar e respeitar outros usuarios durante o contato e a retirada."
                    )
                    LegalSection(
                        title = "Dados coletados",
                        body = "A conta usa nome, email, telefone, cidade, bairro, instituicao, consentimentos, historico de materiais, solicitacoes, notificacoes e trilhas de auditoria para operar o servico com seguranca."
                    )
                    LegalSection(
                        title = "Como os dados sao usados",
                        body = "Esses dados servem para autenticacao, matching geografico, contato entre as partes, envio de notificacoes, prevencao de abuso, exportacao de dados e exclusao/anonimizacao da conta."
                    )
                    LegalSection(
                        title = "IA opcional",
                        body = "O consentimento de IA e separado. Quando ativado, imagens de materiais podem ser analisadas para sugerir classificacao. Quando desativado, o usuario continua podendo preencher os dados manualmente."
                    )
                    LegalSection(
                        title = "Compartilhamento e retencao",
                        body = "Dados de contato sensiveis so devem ser exibidos quando o fluxo da solicitacao exige. O usuario pode revisar, corrigir, exportar e excluir a propria conta pelo app. Ao excluir, materiais e imagens sao removidos do fluxo ativo e os registros necessarios ficam anonimizados."
                    )
                    LegalSection(
                        title = "Aviso importante",
                        body = "Este texto e um rascunho operacional do MVP baseado no comportamento atual do app. Antes de publicacao real, ele ainda deve passar por revisao juridica e de privacidade."
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
