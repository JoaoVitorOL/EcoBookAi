package com.ecobook.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpfValidatorTest {

    @Test
    @DisplayName("isValid should accept any CPF with eleven digits")
    void shouldAcceptCpfsWithElevenDigits() {
        assertThat(CpfValidator.isValid("529.982.247-25")).isTrue();
        assertThat(CpfValidator.isValid("12345678900")).isTrue();
    }

    @Test
    @DisplayName("isValid should reject CPF values with less than eleven digits")
    void shouldRejectShortCpfs() {
        assertThat(CpfValidator.isValid("123.456.789-0")).isFalse();
        assertThat(CpfValidator.isValid("1111111111")).isFalse();
        assertThat(CpfValidator.isValid("5299822472")).isFalse();
    }
}
