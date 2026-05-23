package com.ecobook.ui

import java.text.Normalizer
import java.util.Locale

object ProfileInputRules {

    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun isValidEmail(value: String): Boolean {
        val normalized = value.trim()
        return normalized.isNotBlank() && emailPattern.matches(normalized)
    }

    fun cityStoragePreview(value: String): String {
        val normalized = value.trim()
        if (normalized.isBlank()) {
            return ""
        }

        return Normalizer.normalize(normalized, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .uppercase(Locale.ROOT)
    }
}
