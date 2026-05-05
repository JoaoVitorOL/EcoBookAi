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

    public NormalizedGeo normalize(String city, String neighborhood) {
        return new NormalizedGeo(normalize(city), normalize(neighborhood));
    }

    public record NormalizedGeo(String city, String neighborhood) {
    }
}
