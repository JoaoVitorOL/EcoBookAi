package com.ecobook.ui

object WhatsAppFormatter {

    private val whatsappPattern = Regex("^\\+55\\d{11}$")

    fun format(raw: String): String {
        val digits = raw.filter(Char::isDigit)
        if (digits.isBlank()) {
            return ""
        }

        val normalizedDigits = when {
            digits.startsWith("55") -> digits.take(13)
            else -> "55${digits.take(11)}"
        }

        return "+$normalizedDigits"
    }

    fun formatForInput(raw: String): String {
        return normalizeLocalDigits(raw)
    }

    fun toBackendValue(raw: String): String {
        val localDigits = normalizeLocalDigits(raw)
        return if (localDigits.length == 11) {
            "+55$localDigits"
        } else {
            format(raw)
        }
    }

    fun isValidInput(value: String): Boolean {
        return normalizeLocalDigits(value).length == 11
    }

    fun isValid(value: String): Boolean {
        return whatsappPattern.matches(value.trim())
    }

    private fun normalizeLocalDigits(raw: String): String {
        val digits = raw.filter(Char::isDigit)
        return when {
            digits.startsWith("55") -> digits.drop(2).take(11)
            else -> digits.take(11)
        }
    }
}
