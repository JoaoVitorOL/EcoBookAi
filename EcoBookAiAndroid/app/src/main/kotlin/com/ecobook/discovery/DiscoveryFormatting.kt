package com.ecobook.discovery

import com.ecobook.model.Disciplina
import com.ecobook.model.EstadoConservacao
import com.ecobook.model.NivelEnsino
import com.ecobook.model.SistemaEnsino
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

internal fun formatDisciplina(value: String): String {
    return Disciplina.entries.firstOrNull { it.name == value }?.label ?: humanizeEnum(value)
}

internal fun formatNivelEnsino(value: String): String {
    return NivelEnsino.entries.firstOrNull { it.name == value }?.label ?: humanizeEnum(value)
}

internal fun formatSistemaEnsino(value: String): String {
    return SistemaEnsino.entries.firstOrNull { it.name == value }?.label ?: humanizeEnum(value)
}

internal fun formatEstadoConservacao(value: String): String {
    return EstadoConservacao.entries.firstOrNull { it.name == value }?.label ?: humanizeEnum(value)
}

internal fun formatAnoEscolar(value: Int?): String {
    return when (value) {
        null -> "Nao informado"
        1 -> "1o ano"
        2 -> "2o ano"
        3 -> "3o ano"
        4 -> "4o ano"
        5 -> "5o ano"
        6 -> "6o ano"
        7 -> "7o ano"
        8 -> "8o ano"
        9 -> "9o ano"
        10 -> "1a serie"
        11 -> "2a serie"
        12 -> "3a serie"
        else -> value.toString()
    }
}

internal fun formatRelativeDate(rawValue: String?): String? {
    val localDateTime = parseBackendDateTime(rawValue) ?: return null
    val localDate = localDateTime.toLocalDate()
    val today = java.time.LocalDate.now(ZoneId.systemDefault())
    val daysBetween = ChronoUnit.DAYS.between(localDate, today)

    return when {
        daysBetween <= 0 -> "Hoje"
        daysBetween == 1L -> "Ontem"
        daysBetween in 2..6 -> "$daysBetween dias atras"
        else -> localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR")))
    }
}

internal fun formatAbsoluteDateTime(rawValue: String?): String? {
    val localDateTime = parseBackendDateTime(rawValue) ?: return null
    return localDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'as' HH:mm", Locale("pt", "BR")))
}

private fun parseBackendDateTime(rawValue: String?): LocalDateTime? {
    if (rawValue.isNullOrBlank()) {
        return null
    }

    return try {
        OffsetDateTime.parse(rawValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .atZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime()
    } catch (_: DateTimeParseException) {
        try {
            OffsetDateTime.parse(rawValue)
                .atZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime()
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(rawValue)
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}

private fun humanizeEnum(value: String): String {
    return value
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
