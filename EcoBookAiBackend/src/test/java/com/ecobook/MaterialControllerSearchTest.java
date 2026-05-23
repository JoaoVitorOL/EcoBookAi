package com.ecobook;

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
import com.ecobook.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

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
                .andExpect(jsonPath("$.field_errors.disciplina").value("Valor invalido para disciplina"));

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("after_id", "invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field_errors.after_id").value("Informe um UUID valido para after_id"));

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("min_ano_publicacao", "2025")
                        .param("max_ano_publicacao", "2020"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field_errors.ano_publicacao")
                        .value("O ano minimo de publicacao nao pode ser maior que o maximo"));
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
                        .value("Informe um ano minimo entre 1900 e 2100"));

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("max_ano_publicacao", "2200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field_errors.max_ano_publicacao")
                        .value("Informe um ano maximo entre 1900 e 2100"));
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

    private Usuario createUser(String email, String cidade, String bairro) {
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .email(email)
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Search User")
                .whatsapp("+5511991234567")
                .cidade(cidade)
                .bairro(bairro)
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
