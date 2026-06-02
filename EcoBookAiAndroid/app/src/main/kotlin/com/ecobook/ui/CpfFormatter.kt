package com.ecobook.ui

object CpfFormatter {

    private const val CPF_LENGTH = 11

    fun formatForInput(value: String): String {
        return value.filter(Char::isDigit).take(CPF_LENGTH)
    }

    fun toBackendValue(value: String): String {
        return value.filter(Char::isDigit)
    }

    fun isValidInput(value: String): Boolean {
        return toBackendValue(value).length == CPF_LENGTH
    }
}
