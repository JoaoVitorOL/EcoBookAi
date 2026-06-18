package com.ecobook.validator;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Component
public class WhatsAppValidator {

    private static final Pattern BRAZIL_E164_PATTERN = Pattern.compile("^\\+55\\d{11}$");

    /**
     * Validates whether the provided WhatsApp number matches the accepted format.
     * @param whatsapp WhatsApp number to validate
     * @return true when the condition holds; otherwise false
     */
    public boolean isValid(String whatsapp) {
        return StringUtils.hasText(whatsapp) && BRAZIL_E164_PATTERN.matcher(whatsapp).matches();
    }
}
