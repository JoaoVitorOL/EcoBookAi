package com.ecobook.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ecobook.ui.LoadingSpinner
import com.ecobook.ui.components.GlassCard

@Composable
fun ProcessingScreen(selectedImage: SelectedImageUiModel?) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        LoadingSpinner(
            title = "Analisando seu material",
            subtitle = "Estamos validando a imagem, preparando o upload e consultando a IA. Esse passo costuma levar ate 10 segundos."
        )

        selectedImage?.let { image ->
            GlassCard {
                Text(
                    text = "Arquivo em processamento",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = image.fileName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Se a IA falhar ou o consentimento estiver desligado, voce ainda podera preencher tudo manualmente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5F746B)
                )
            }
        }
    }
}
