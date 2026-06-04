package com.ecobook;

import com.ecobook.model.Material;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Disciplina;
import com.ecobook.model.enums.EstadoConservacao;
import com.ecobook.model.enums.NecessidadeAcademica;
import com.ecobook.model.enums.NivelEnsino;
import com.ecobook.model.enums.Role;
import com.ecobook.model.enums.SistemaEnsino;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaterialControllerSearchTest extends BaseIntegrationTest {

    private static final String SEEDED_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHiPpclJ3DgA6hfa/vw/jemjL5V6cO9e";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("GET /api/v1/materiais should return paginated discovery results")
    void shouldReturnPaginatedResults() throws Exception {
        Usuario requester = createUser("searcher@example.com", "SAO PAULO", "CENTRO");
        Usuario donor = createUser("donor@example.com", "SAO PAULO", "LIBERDADE");

        createMaterial(donor, "Material 1", 2020);
        createMaterial(donor, "Material 2", 2021);
        createMaterial(donor, "Material 3", 2022);

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("disciplina", "MATEMATICA")
                        .param("nivel_ensino", "FUNDAMENTAL")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.has_next").value(true))
                .andExpect(jsonPath("$.data.results.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/materiais should return a non-first page slice using zero-based pagination")
    void shouldReturnNonFirstPageSlice() throws Exception {
        Usuario requester = createUser("paged-searcher@example.com", "SAO PAULO", "CENTRO");
        Usuario donor = createUser("paged-donor@example.com", "SAO PAULO", "CENTRO");

        createMaterial(donor, "Base 2020", 2020);
        createMaterial(donor, "Base 2021", 2021);
        createMaterial(donor, "Base 2022", 2022);

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.results.length()").value(1))
                .andExpect(jsonPath("$.data.results[0].titulo").value("Base 2021"));
    }

    @Test
    @DisplayName("GET /api/v1/materiais should support keyset pagination via after_id")
    void shouldSupportKeysetPagination() throws Exception {
        Usuario requester = createUser("cursor-searcher@example.com", "FLORIANOPOLIS", "CENTRO");
        Usuario donor = createUser("cursor-donor@example.com", "FLORIANOPOLIS", "CENTRO");

        Material olderMaterial = createMaterial(donor, "Colecao 2020", 2020);
        Material middleMaterial = createMaterial(donor, "Colecao 2021", 2021);
        Material newerMaterial = createMaterial(donor, "Colecao 2022", 2022);

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination_mode").value("OFFSET"))
                .andExpect(jsonPath("$.data.results[0].titulo").value("Colecao 2022"))
                .andExpect(jsonPath("$.data.next_after_id").value(newerMaterial.getId().toString()));

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("after_id", newerMaterial.getId().toString())
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pagination_mode").value("KEYSET"))
                .andExpect(jsonPath("$.data.after_id").value(newerMaterial.getId().toString()))
                .andExpect(jsonPath("$.data.next_after_id").value(middleMaterial.getId().toString()))
                .andExpect(jsonPath("$.data.results.length()").value(1))
                .andExpect(jsonPath("$.data.results[0].titulo").value("Colecao 2021"));

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("after_id", middleMaterial.getId().toString())
                        .param("page", "2")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.pagination_mode").value("KEYSET"))
                .andExpect(jsonPath("$.data.results[0].titulo").value("Colecao 2020"))
                .andExpect(jsonPath("$.data.next_after_id").doesNotExist())
                .andExpect(jsonPath("$.data.has_next").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/materiais should reject invalid enums and publication ranges")
    void shouldRejectInvalidFilters() throws Exception {
        Usuario requester = createUser("invalid-search@example.com", "SAO PAULO", "CENTRO");

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("disciplina", "INVALIDA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_FORMAT"))
                .andExpect(jsonPath("$.field_errors.disciplina").value("Valor inválido para disciplina"));

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("after_id", "invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field_errors.after_id").value("Informe um UUID válido para after_id"));

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("min_ano_publicacao", "2025")
                        .param("max_ano_publicacao", "2020"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field_errors.ano_publicacao")
                        .value("O ano mínimo de publicação não pode ser maior que o máximo"));
    }

    @Test
    @DisplayName("GET /api/v1/materiais should reject publication years outside supported bounds")
    void shouldRejectOutOfBoundsPublicationYears() throws Exception {
        Usuario requester = createUser("bounds-search@example.com", "SAO PAULO", "CENTRO");

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("min_ano_publicacao", "1800"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field_errors.min_ano_publicacao")
                        .value("Informe um ano mínimo entre 1900 e 2100"));

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("max_ano_publicacao", "2200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field_errors.max_ano_publicacao")
                        .value("Informe um ano máximo entre 1900 e 2100"));
    }

    @Test
    @DisplayName("GET /api/v1/materiais should support accent-insensitive text queries")
    void shouldSupportTextQuery() throws Exception {
        Usuario requester = createUser("query-search@example.com", "FLORIANOPOLIS", "CENTRO");
        Usuario donor = createUser("query-donor@example.com", "FLORIANOPOLIS", "TRINDADE");

        createMaterial(donor, "Algebra Linear", 2023);
        createMaterial(donor, "Historia Antiga", 2020);

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("query", "algebra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.results[0].titulo").value("Algebra Linear"));
    }

    @Test
    @DisplayName("GET /api/v1/materiais should filter by city and neighborhood with accent-insensitive matching")
    void shouldFilterByCityAndNeighborhood() throws Exception {
        Usuario requester = createUser("geo-search@example.com", "FLORIANOPOLIS", "CENTRO");
        Usuario centroDonor = createUser("geo-centro@example.com", "FLORIANOPOLIS", "Centro Histórico");
        Usuario trindadeDonor = createUser("geo-trindade@example.com", "FLORIANOPOLIS", "Trindade");
        Usuario otherCityDonor = createUser("geo-other-city@example.com", "SAO JOSE", "Centro Histórico");

        createMaterial(centroDonor, "Colecao Centro Historico", 2024);
        createMaterial(trindadeDonor, "Colecao Trindade", 2023);
        createMaterial(otherCityDonor, "Colecao Sao Jose", 2022);

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("cidade", "Florianópolis")
                        .param("bairro", "centro historico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.results.length()").value(1))
                .andExpect(jsonPath("$.data.results[0].titulo").value("Colecao Centro Historico"));
    }

    @Test
    @DisplayName("GET /api/v1/materiais should filter by publication year range")
    void shouldFilterByPublicationYearRange() throws Exception {
        Usuario requester = createUser("range-search@example.com", "FLORIANOPOLIS", "CENTRO");
        Usuario donor = createUser("range-donor@example.com", "FLORIANOPOLIS", "CENTRO");

        createMaterial(donor, "Colecao 2008", 2008);
        createMaterial(donor, "Colecao 2012", 2012);
        createMaterial(donor, "Colecao 2018", 2018);

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("min_ano_publicacao", "2010")
                        .param("max_ano_publicacao", "2015"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.results[0].titulo").value("Colecao 2012"));
    }

    @Test
    @DisplayName("GET /api/v1/materiais should prioritize exact discipline results before multidiscipline ones")
    void shouldPrioritizeExactDisciplineResults() throws Exception {
        Usuario requester = createUser("discipline-priority@example.com", "CRICIUMA", "CENTRO");
        Usuario donor = createUser("discipline-priority-donor@example.com", "CRICIUMA", "CENTRO");

        materialRepository.saveAndFlush(Material.builder()
                .doador(donor)
                .titulo("Colecao Todas Mais Nova")
                .descricao("Descricao de descoberta para Colecao Todas Mais Nova")
                .disciplina(Disciplina.TODAS)
                .nivelEnsino(NivelEnsino.FUNDAMENTAL)
                .ano(7)
                .sistemaEnsino(SistemaEnsino.ANGLO)
                .estadoConservacao(EstadoConservacao.BOM)
                .necessidadeAcademica(NecessidadeAcademica.TEXTBOOKS)
                .status(StatusMaterial.DISPONIVEL)
                .cidade(donor.getCidade())
                .bairro(donor.getBairro())
                .dataPublicacao(2025)
                .build());

        materialRepository.saveAndFlush(Material.builder()
                .doador(donor)
                .titulo("Literatura Exata")
                .descricao("Descricao de descoberta para Literatura Exata")
                .disciplina(Disciplina.LITERATURA)
                .nivelEnsino(NivelEnsino.FUNDAMENTAL)
                .ano(7)
                .sistemaEnsino(SistemaEnsino.ANGLO)
                .estadoConservacao(EstadoConservacao.BOM)
                .necessidadeAcademica(NecessidadeAcademica.TEXTBOOKS)
                .status(StatusMaterial.DISPONIVEL)
                .cidade(donor.getCidade())
                .bairro(donor.getBairro())
                .dataPublicacao(2021)
                .build());

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("disciplina", "LITERATURA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].titulo").value("Literatura Exata"))
                .andExpect(jsonPath("$.data.results[1].titulo").value("Colecao Todas Mais Nova"));
    }

    @Test
    @DisplayName("GET /api/v1/materiais should require an exact sistema_ensino match")
    void shouldRequireExactSystemMatch() throws Exception {
        Usuario requester = createUser("system-search@example.com", "CURITIBA", "CENTRO");
        Usuario donor = createUser("system-donor@example.com", "CURITIBA", "CENTRO");

        createMaterial(donor, "Anglo Exato", 2020);
        materialRepository.saveAndFlush(Material.builder()
                .doador(donor)
                .titulo("Outro Excluido")
                .descricao("Descricao de descoberta para Outro Excluido")
                .disciplina(Disciplina.MATEMATICA)
                .nivelEnsino(NivelEnsino.FUNDAMENTAL)
                .ano(7)
                .sistemaEnsino(SistemaEnsino.OUTRO)
                .estadoConservacao(EstadoConservacao.BOM)
                .status(StatusMaterial.DISPONIVEL)
                .cidade(donor.getCidade())
                .bairro(donor.getBairro())
                .dataPublicacao(2021)
                .build());

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("sistema_ensino", "ANGLO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.results[0].titulo").value("Anglo Exato"));
    }

    @Test
    @DisplayName("GET /api/v1/materiais should filter by academic need")
    void shouldFilterByAcademicNeed() throws Exception {
        Usuario requester = createUser("needs-search@example.com", "CURITIBA", "CENTRO");
        Usuario donor = createUser("needs-donor@example.com", "CURITIBA", "CENTRO");

        createMaterial(donor, "Colecao Escolar 2024", 2024);
        createMaterial(donor, "Guia Enem 2020", 2020);

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("necessidade_academica", "TEST_PREP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].titulo").value("Guia Enem 2020"));
    }

    @Test
    @DisplayName("GET /api/v1/materiais should keep paginating correctly with more than 1000 materials")
    void shouldHandleLargeDiscoveryWindows() throws Exception {
        Usuario requester = createUser("stress-search@example.com", "FLORIANOPOLIS", "CENTRO");
        Usuario donor = createUser("stress-donor@example.com", "FLORIANOPOLIS", "CENTRO");

        List<Material> materials = new ArrayList<>();
        for (int index = 0; index < 1001; index++) {
            materials.add(Material.builder()
                    .doador(donor)
                    .titulo("Colecao stress " + index)
                    .descricao("Descricao de stress para colecao " + index)
                    .disciplina(Disciplina.MATEMATICA)
                    .nivelEnsino(NivelEnsino.FUNDAMENTAL)
                    .ano(7)
                    .sistemaEnsino(SistemaEnsino.ANGLO)
                    .estadoConservacao(EstadoConservacao.BOM)
                    .status(StatusMaterial.DISPONIVEL)
                    .cidade(donor.getCidade())
                    .bairro(donor.getBairro())
                    .dataPublicacao(2000 + (index % 20))
                    .build());
        }
        materialRepository.saveAllAndFlush(materials);

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("page", "0")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1001))
                .andExpect(jsonPath("$.data.results.length()").value(25))
                .andExpect(jsonPath("$.data.has_next").value(true))
                .andExpect(jsonPath("$.data.next_after_id").isNotEmpty());
    }

    private Usuario createUser(String email, String cidade, String bairro) {
        return createUser(email, cidade, bairro, Set.of());
    }

    private Usuario createUser(String email,
                               String cidade,
                               String bairro,
                               Set<NecessidadeAcademica> necessidadesAcademicas) {
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .email(email)
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Search User")
                .whatsapp("+5511991234567")
                .cpf("12345678909")
                .cidade(cidade)
                .bairro(bairro)
                .necessidadesAcademicas(necessidadesAcademicas)
                .perfilCompleto(true)
                .consentimentoIa(true)
                .role(Role.USER)
                .build());
    }

    private Material createMaterial(Usuario donor, String titulo, Integer dataPublicacao) {
        return materialRepository.saveAndFlush(Material.builder()
                .doador(donor)
                .titulo(titulo)
                .descricao("Descricao de descoberta para " + titulo)
                .disciplina(Disciplina.MATEMATICA)
                .nivelEnsino(NivelEnsino.FUNDAMENTAL)
                .ano(7)
                .sistemaEnsino(SistemaEnsino.ANGLO)
                .estadoConservacao(EstadoConservacao.BOM)
                .necessidadeAcademica(titulo.contains("Enem")
                        ? NecessidadeAcademica.TEST_PREP
                        : NecessidadeAcademica.TEXTBOOKS)
                .status(StatusMaterial.DISPONIVEL)
                .cidade(donor.getCidade())
                .bairro(donor.getBairro())
                .dataPublicacao(dataPublicacao)
                .build());
    }

    private String tokenFor(Usuario usuario) {
        return jwtTokenProvider.generateToken(
                usuario.getEmail(),
                usuario.getRole().name(),
                usuario.isPerfilCompleto(),
                usuario.getId().toString()
        );
    }
}
