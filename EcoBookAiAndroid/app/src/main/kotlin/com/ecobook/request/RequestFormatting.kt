package com.ecobook.request

import androidx.compose.ui.graphics.Color
import java.util.Locale

internal fun formatRequestStatus(status: String): String {
    return when (status) {
        "PENDENTE" -> "Pendente"
        "APROVADA" -> "Aprovada"
        "RECUSADA" -> "Recusada"
        "CANCELADA" -> "Cancelada"
        "CONCLUIDA" -> "Concluida"
        else -> status
            .split('_')
            .joinToString(" ") { token ->
                token.lowercase().replaceFirstChar { char ->
                    if (char.isLowerCase()) {
                        char.titlecase(Locale("pt", "BR"))
                    } else {
                        char.toString()
                    }
                }
            }
    }
}

internal fun requestStatusColors(status: String): Pair<Color, Color> {
    return when (status) {
        "PENDENTE" -> Color(0xFFFCE7D8) to Color(0xFF8A4C1F)
        "APROVADA" -> Color(0xFFE5F0EA) to Color(0xFF205447)
        "RECUSADA" -> Color(0xFFF7DDDB) to Color(0xFF8D3D30)
        "CANCELADA", "CONCLUIDA" -> Color(0xFFF0F1F3) to Color(0xFF4B5563)
        else -> Color(0xFFF0F1F3) to Color(0xFF4B5563)
    }
}
