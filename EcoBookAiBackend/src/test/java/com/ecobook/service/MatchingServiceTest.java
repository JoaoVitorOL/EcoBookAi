package com.ecobook.service;

import com.ecobook.BaseIntegrationTest;
import com.ecobook.dto.MaterialDTO;
import com.ecobook.dto.PagedResponseDTO;
import com.ecobook.dto.SearchCriteriaDTO;
import com.ecobook.model.Material;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Disciplina;
import com.ecobook.model.enums.EstadoConservacao;
import com.ecobook.model.enums.NivelEnsino;
import com.ecobook.model.enums.Role;
import com.ecobook.model.enums.SistemaEnsino;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingServiceTest extends BaseIntegrationTest {

    private static final String SEEDED_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHiPpclJ3DgA6hfa/vw/jemjL5V6cO9e";

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Test
    @DisplayName("findMatching should rank same neighborhood first, then same city, then newest publication")
    void shouldRankByNeighborhoodThenCityThenPublicationYear() {
        Usuario centroDonor = createUser("centro-donor@example.com", "SAO PAULO", "CENTRO");
        Usuario liberdadeDonor = createUser("liberdade-donor@example.com", "SAO PAULO", "LIBERDADE");
        Usuario campinasDonor = createUser("campinas-donor@example.com", "CAMPINAS", "CAMBUCI");

        createMaterial(centroDonor, "Anglo 2020", Disciplina.MATEMATICA, NivelEnsino.FUNDAMENTAL, 5, SistemaEnsino.ANGLO, "SAO PAULO", "CENTRO", 2020);
        createMaterial(centroDonor, "Outro 2024", Disciplina.MATEMATICA, NivelEnsino.FUNDAMENTAL, 6, SistemaEnsino.OUTRO, "SAO PAULO", "CENTRO", 2024);
        createMaterial(liberdadeDonor, "Anglo 2023", Disciplina.MATEMATICA, NivelEnsino.FUNDAMENTAL, 4, SistemaEnsino.ANGLO, "SAO PAULO", "LIBERDADE", 2023);
        createMaterial(campinasDonor, "Anglo 2025", Disciplina.MATEMATICA, NivelEnsino.FUNDAMENTAL, 5, SistemaEnsino.ANGLO, "CAMPINAS", "CAMBUCI", 2025);
        createMaterial(centroDonor, "Positivo excluido", Disciplina.MATEMATICA, NivelEnsino.FUNDAMENTAL, 5, SistemaEnsino.POSITIVO, "SAO PAULO", "CENTRO", 2026);
        createMaterial(centroDonor, "Historia excluida", Disciplina.HISTORIA, NivelEnsino.FUNDAMENTAL, 5, SistemaEnsino.ANGLO, "SAO PAULO", "CENTRO", 2026);

        PagedResponseDTO<MaterialDTO> response = matchingService.findMatching(
                SearchCriteriaDTO.builder()
                        .disciplina(Disciplina.MATEMATICA)
                        .nivelEnsino(NivelEnsino.FUNDAMENTAL)
                        .ano(5)
                        .sistemaEnsino(SistemaEnsino.ANGLO)
                        .cidade("Sao Paulo")
                        .bairro("Centro")
                        .build(),
                PageRequest.of(0, 10)
        );

        assertThat(response.getTotal()).isEqualTo(4);
        assertThat(response.getResults()).extracting(MaterialDTO::getTitulo)
                .containsExactly("Outro 2024", "Anglo 2020", "Anglo 2023", "Anglo 2025");
    }

    @Test
    @DisplayName("findMatching should support accent-insensitive text queries and keep superior materials when year is provided")
    void shouldSupportQueryAndSuperiorYearRule() {
        Usuario donor = createUser("query-donor@example.com", "FLORIANOPOLIS", "CENTRO");

        createMaterial(donor, "Álgebra Fundamental", Disciplina.MATEMATICA, NivelEnsino.FUNDAMENTAL, 7, SistemaEnsino.OUTRO, "FLORIANOPOLIS", "CENTRO", 2022);
        createMaterial(donor, "Historia ano 9", Disciplina.HISTORIA, NivelEnsino.FUNDAMENTAL, 9, SistemaEnsino.OUTRO, "FLORIANOPOLIS", "CENTRO", 2021);
        createMaterial(donor, "Calculo Superior", Disciplina.MATEMATICA, NivelEnsino.SUPERIOR, null, SistemaEnsino.OUTRO, "FLORIANOPOLIS", "CENTRO", 2020);

        PagedResponseDTO<MaterialDTO> response = matchingService.findMatching(
                SearchCriteriaDTO.builder()
                        .query("algebra")
                        .ano(6)
                        .build(),
                PageRequest.of(0, 10)
        );

        assertThat(response.getResults()).extracting(MaterialDTO::getTitulo)
                .containsExactly("Álgebra Fundamental");

        PagedResponseDTO<MaterialDTO> superiorAwareResponse = matchingService.findMatching(
                SearchCriteriaDTO.builder()
                        .ano(6)
                        .build(),
                PageRequest.of(0, 10)
        );

        assertThat(superiorAwareResponse.getResults()).extracting(MaterialDTO::getTitulo)
                .contains("Calculo Superior")
                .doesNotContain("Historia ano 9");
    }

    @Test
    @DisplayName("findMatching should enforce OUTRO system rule and publication year range")
    void shouldApplySystemRuleAndPublicationRange() {
        Usuario donor = createUser("system-donor@example.com", "CURITIBA", "CENTRO");

        createMaterial(donor, "Outro 2018", Disciplina.MATEMATICA, NivelEnsino.FUNDAMENTAL, 7, SistemaEnsino.OUTRO, "CURITIBA", "CENTRO", 2018);
        createMaterial(donor, "Anglo 2019", Disciplina.MATEMATICA, NivelEnsino.FUNDAMENTAL, 7, SistemaEnsino.ANGLO, "CURITIBA", "CENTRO", 2019);
        createMaterial(donor, "Outro 2021", Disciplina.MATEMATICA, NivelEnsino.FUNDAMENTAL, 7, SistemaEnsino.OUTRO, "CURITIBA", "CENTRO", 2021);
        createMaterial(donor, "Positivo 2019", Disciplina.MATEMATICA, NivelEnsino.FUNDAMENTAL, 7, SistemaEnsino.POSITIVO, "CURITIBA", "CENTRO", 2019);

        PagedResponseDTO<MaterialDTO> outroOnlyResponse = matchingService.findMatching(
                SearchCriteriaDTO.builder()
                        .sistemaEnsino(SistemaEnsino.OUTRO)
                        .minAnoPublicacao(2019)
                        .maxAnoPublicacao(2021)
                        .build(),
                PageRequest.of(0, 10)
        );

        assertThat(outroOnlyResponse.getResults()).extracting(MaterialDTO::getTitulo)
                .containsExactly("Outro 2021");

        PagedResponseDTO<MaterialDTO> angloWithOutroFallbackResponse = matchingService.findMatching(
                SearchCriteriaDTO.builder()
                        .sistemaEnsino(SistemaEnsino.ANGLO)
                        .minAnoPublicacao(2018)
                        .maxAnoPublicacao(2019)
                        .build(),
                PageRequest.of(0, 10)
        );

        assertThat(angloWithOutroFallbackResponse.getResults()).extracting(MaterialDTO::getTitulo)
                .containsExactly("Anglo 2019", "Outro 2018");
    }

    private Usuario createUser(String email, String cidade, String bairro) {
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .email(email)
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Matching User")
                .whatsapp("+5511991234567")
                .cidade(cidade)
                .bairro(bairro)
                .perfilCompleto(true)
                .consentimentoIa(true)
                .role(Role.USER)
                .build());
    }

    private Material createMaterial(Usuario donor,
                                    String titulo,
                                    Disciplina disciplina,
                                    NivelEnsino nivelEnsino,
                                    Integer ano,
                                    SistemaEnsino sistemaEnsino,
                                    String cidade,
                                    String bairro,
                                    Integer dataPublicacao) {
        return materialRepository.saveAndFlush(Material.builder()
                .doador(donor)
                .titulo(titulo)
                .descricao("Descricao de apoio para " + titulo)
                .disciplina(disciplina)
                .nivelEnsino(nivelEnsino)
                .ano(ano)
                .sistemaEnsino(sistemaEnsino)
                .estadoConservacao(EstadoConservacao.BOM)
                .status(StatusMaterial.DISPONIVEL)
                .cidade(cidade)
                .bairro(bairro)
                .dataPublicacao(dataPublicacao)
                .build());
    }
}
