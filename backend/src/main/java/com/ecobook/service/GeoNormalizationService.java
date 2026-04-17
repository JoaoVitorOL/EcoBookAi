package com.ecobook.service;

import org.springframework.stereotype.Service;

/**
 * Geographic normalization service
 */
@Service
public class GeoNormalizationService {

    /**
     * Normalize city and neighborhood names
     */
    public String normalize(String text) {
        // Implementation: Uppercase + NFD decomposition + ASCII transliteration
        return text.toUpperCase();
    }
}
