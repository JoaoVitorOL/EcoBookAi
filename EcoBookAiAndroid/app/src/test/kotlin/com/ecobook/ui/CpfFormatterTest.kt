package com.ecobook.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CpfFormatterTest {

    @Test
    fun isValidInputShouldAcceptAnyCpfWithElevenDigits() {
        assertTrue(CpfFormatter.isValidInput("529.982.247-25"))
        assertTrue(CpfFormatter.isValidInput("12345678900"))
    }

    @Test
    fun isValidInputShouldRejectCpfWithLessThanElevenDigits() {
        assertFalse(CpfFormatter.isValidInput("123.456.789-0"))
        assertFalse(CpfFormatter.isValidInput("1111111111"))
        assertFalse(CpfFormatter.isValidInput("5299822472"))
    }
}
