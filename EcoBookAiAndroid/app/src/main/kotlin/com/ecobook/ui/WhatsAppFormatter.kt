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

    fun isValid(value: String): Boolean {
        return whatsappPattern.matches(value.trim())
    }
}
