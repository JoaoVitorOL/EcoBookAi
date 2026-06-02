package com.ecobook.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

fun TextFieldValue.digitsOnly(maxDigits: Int): TextFieldValue {
    val filteredText = text.filter(Char::isDigit).take(maxDigits)
    val filteredCursor = text
        .take(selection.end.coerceAtMost(text.length))
        .filter(Char::isDigit)
        .take(maxDigits)
        .length

    return TextFieldValue(
        text = filteredText,
        selection = TextRange(filteredCursor)
    )
}
