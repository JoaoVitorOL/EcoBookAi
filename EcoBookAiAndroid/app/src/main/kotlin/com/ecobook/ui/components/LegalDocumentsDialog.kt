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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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
                    text = "Resumo do comportamento atual do EcoBook antes do aceite da plataforma.",
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
                        body = "O EcoBook conecta doadores e responsáveis interessados em materiais de estudo. A conta deve ser usada por pais, mães e responsáveis legais, nunca por crianças ou adolescentes sozinhos."
                    )
                    LegalSection(
                        title = "Fluxo do aplicativo",
                        body = "O perfil libera os recursos protegidos. Cada material é revisado manualmente, inclusive a necessidade acadêmica. A busca usa filtros reais e o pedido é aprovado dentro do app antes de qualquer contato externo."
                    )
                    LegalSection(
                        title = "IA opcional",
                        body = "Se o consentimento para IA estiver ativo, as imagens podem ser analisadas para sugerir metadados do material. A publicação continua dependendo de revisão humana e a necessidade acadêmica segue sendo escolhida manualmente."
                    )
                    LegalSection(
                        title = "Entrega e contato",
                        body = "O app não organiza conversa, retirada nem ponto de encontro dentro da plataforma. Depois da aprovação, doador e responsável combinam tudo diretamente pelo WhatsApp, por conta própria e fora do EcoBook."
                    )
                    LegalSection(
                        title = "Privacidade e LGPD",
                        body = "O serviço trata dados como nome, email, WhatsApp, CPF, cidade, bairro, materiais, solicitações e notificações para autenticação, segurança, busca e exclusão da conta. O titular pode pedir acesso, correção, eliminação e revogação de consentimento, conforme a LGPD e os canais efetivamente oferecidos pelo produto e pelo operador responsável."
                    )
                    LegalSection(
                        title = "Aviso importante",
                        body = "Este texto é um rascunho operacional do MVP baseado no comportamento atual do app. Antes de uma publicação real, ele ainda deve passar por revisão jurídica, definição do controlador e publicação do canal oficial de privacidade."
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
