package com.ecobook.validator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsAppValidatorTest {

    private final WhatsAppValidator validator = new WhatsAppValidator();

    @Test
    void shouldAcceptBrazilianE164WhatsappNumbers() {
        assertThat(validator.isValid("+5511991234567")).isTrue();
        assertThat(validator.isValid("+5548991234567")).isTrue();
    }

    @Test
    void shouldRejectInvalidWhatsappFormats() {
        assertThat(validator.isValid("11991234567")).isFalse();
        assertThat(validator.isValid("+55 11 991234567")).isFalse();
        assertThat(validator.isValid("+551199123456")).isFalse();
        assertThat(validator.isValid("+55119912345")).isFalse();
    }
}
