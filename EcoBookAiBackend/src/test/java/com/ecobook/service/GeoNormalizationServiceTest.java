package com.ecobook.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeoNormalizationServiceTest {

    private final GeoNormalizationService geoNormalizationService = new GeoNormalizationService();

    @Test
    @DisplayName("normalize should return null when the source text is null")
    void shouldReturnNullForNullInput() {
        assertThat(geoNormalizationService.normalize((String) null)).isNull();
    }

    @Test
    @DisplayName("normalize should uppercase, trim, and remove accents from a city name")
    void shouldNormalizeCityName() {
        assertThat(geoNormalizationService.normalize("  S\u00E3o Jos\u00E9 dos Campos  "))
                .isEqualTo("SAO JOSE DOS CAMPOS");
    }

    @Test
    @DisplayName("normalize should collapse repeated whitespace and keep ASCII-only output")
    void shouldCollapseWhitespaceAndRemoveAccents() {
        assertThat(geoNormalizationService.normalize("  centro   hist\u00F3rico  "))
                .isEqualTo("CENTRO HISTORICO");
    }

    @Test
    @DisplayName("normalize should process city and neighborhood together")
    void shouldNormalizeCityAndNeighborhoodTogether() {
        GeoNormalizationService.NormalizedGeo normalizedGeo = geoNormalizationService.normalize(
                "Florian\u00F3polis",
                "  Lagoa da Concei\u00E7\u00E3o "
        );

        assertThat(normalizedGeo.city()).isEqualTo("FLORIANOPOLIS");
        assertThat(normalizedGeo.neighborhood()).isEqualTo("LAGOA DA CONCEICAO");
    }
}
