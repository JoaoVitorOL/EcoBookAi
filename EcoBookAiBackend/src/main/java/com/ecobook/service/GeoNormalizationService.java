package com.ecobook.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Geographic normalization service
 */
@Service
public class GeoNormalizationService {

    /**
     * Normalize city and neighborhood names
     */
    public String normalize(String text) {
        if (text == null) {
            return null;
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isEmpty()) {
            return normalized;
        }

        return normalized.toUpperCase(Locale.ROOT);
    }

    /**
     * Normalizes the city and sanitizes the neighborhood text for geographic matching.
     * @param city city text to normalize
     * @param neighborhood neighborhood text to sanitize
     * @return result of the operation
     */
    public NormalizedGeo normalize(String city, String neighborhood) {
        return new NormalizedGeo(normalize(city), sanitizeDisplayText(neighborhood));
    }

    /**
     * Trims and normalizes display text without removing accents.
     * @param text input text to sanitize or normalize
     * @return result of the operation
     */
    public String sanitizeDisplayText(String text) {
        if (text == null) {
            return null;
        }

        return text.replaceAll("\\s+", " ").trim();
    }

    public record NormalizedGeo(String city, String neighborhood) {
    }
}
