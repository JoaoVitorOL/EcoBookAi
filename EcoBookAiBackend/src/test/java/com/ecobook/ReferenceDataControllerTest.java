package com.ecobook;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReferenceDataControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/reference-data/material-options should expose immutable catalogs without authentication")
    void shouldReturnMaterialOptionsCatalog() throws Exception {
        mockMvc.perform(get("/v1/reference-data/material-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Catalogos de referencia carregados com sucesso"))
                .andExpect(jsonPath("$.data.disciplinas[0].value").value("TODAS"))
                .andExpect(jsonPath("$.data.disciplinas[1].label").value("Matematica"))
                .andExpect(jsonPath("$.data.niveis_ensino[1].value").value("MEDIO"))
                .andExpect(jsonPath("$.data.sistemas_ensino[0].value").value("ANGLO"))
                .andExpect(jsonPath("$.data.estados_conservacao[0].value").value("NOVO"))
                .andExpect(jsonPath("$.data.necessidades_academicas[0].value").value("TEXTBOOKS"))
                .andExpect(jsonPath("$.data.necessidades_academicas[0].label").value("Livros didaticos"));
    }
}
