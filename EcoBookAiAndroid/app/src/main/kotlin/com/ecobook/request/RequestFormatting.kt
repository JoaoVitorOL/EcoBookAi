package com.ecobook.request

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ecobook.ui.theme.EcoBookTone
import com.ecobook.ui.theme.ecoBookBadgeColors
import java.util.Locale

internal fun formatRequestStatus(status: String): String {
    return when (status) {
        "PENDENTE" -> "Pendente"
        "APROVADA" -> "Aprovada"
        "RECUSADA" -> "Recusada"
        "CANCELADA" -> "Cancelada"
        "CONCLUIDA" -> "Concluída"
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

@Composable
internal fun requestStatusColors(status: String): Pair<Color, Color> {
    val colors = ecoBookBadgeColors(
        when (status) {
            "PENDENTE" -> EcoBookTone.Warning
            "APROVADA" -> EcoBookTone.Success
            "RECUSADA" -> EcoBookTone.Danger
            "CANCELADA", "CONCLUIDA" -> EcoBookTone.Neutral
            else -> EcoBookTone.Neutral
        }
    )
    return colors.containerColor to colors.contentColor
}
