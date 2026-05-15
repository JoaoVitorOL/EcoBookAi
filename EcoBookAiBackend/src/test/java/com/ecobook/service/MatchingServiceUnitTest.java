package com.ecobook.service;

import com.ecobook.dto.MaterialDTO;
import com.ecobook.dto.PagedResponseDTO;
import com.ecobook.dto.SearchCriteriaDTO;
import com.ecobook.model.Material;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Disciplina;
import com.ecobook.model.enums.EstadoConservacao;
import com.ecobook.model.enums.NivelEnsino;
import com.ecobook.model.enums.SistemaEnsino;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.repository.MaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingServiceUnitTest {

    @Mock
    private MaterialRepository materialRepository;

    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        matchingService = new MatchingService(materialRepository, new GeoNormalizationService(), new MaterialMapper());
    }

    @Test
    @DisplayName("findMatching should require exact sistema_ensino matching")
    void shouldRequireExactSystemMatch() {
        when(materialRepository.findByStatus(StatusMaterial.DISPONIVEL)).thenReturn(List.of(
                material("Anglo Exato", SistemaEnsino.ANGLO, 2021),
                material("Outro Excluido", SistemaEnsino.OUTRO, 2022)
        ));

        PagedResponseDTO<MaterialDTO> response = matchingService.findMatching(
                SearchCriteriaDTO.builder()
                        .sistemaEnsino(SistemaEnsino.ANGLO)
                        .build(),
                PageRequest.of(0, 10)
        );

        assertThat(response.getResults()).extracting(MaterialDTO::getTitulo)
                .containsExactly("Anglo Exato");
    }

    private Material material(String titulo, SistemaEnsino sistemaEnsino, int dataPublicacao) {
        Usuario donor = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Pessoa Doadora")
                .whatsapp("+5511991234567")
                .cidade("CURITIBA")
                .bairro("CENTRO")
                .build();

        return Material.builder()
                .id(UUID.randomUUID())
                .doador(donor)
                .titulo(titulo)
                .descricao("Descricao de teste")
                .disciplina(Disciplina.MATEMATICA)
                .nivelEnsino(NivelEnsino.FUNDAMENTAL)
                .ano(7)
                .sistemaEnsino(sistemaEnsino)
                .estadoConservacao(EstadoConservacao.BOM)
                .status(StatusMaterial.DISPONIVEL)
                .cidade("CURITIBA")
                .bairro("CENTRO")
                .dataPublicacao(dataPublicacao)
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
    }
}
