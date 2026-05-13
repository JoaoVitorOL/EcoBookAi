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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaterialControllerOwnerTest extends BaseIntegrationTest {

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
    @DisplayName("GET /api/v1/materiais/me should list only the current donor materials in reverse creation order")
    void shouldListCurrentUserMaterials() throws Exception {
        Usuario owner = createUser("owner-list@example.com");
        Usuario other = createUser("other-list@example.com");

        Material oldest = createMaterial(owner, "Colecao antiga", StatusMaterial.DISPONIVEL, LocalDateTime.now().minusDays(3));
        Material newest = createMaterial(owner, "Colecao nova", StatusMaterial.CANCELADO, LocalDateTime.now().minusDays(1));
        createMaterial(other, "Nao deve aparecer", StatusMaterial.DISPONIVEL, LocalDateTime.now());

        mockMvc.perform(get("/v1/materiais/me")
                        .header("Authorization", "Bearer " + tokenFor(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(newest.getId().toString()))
                .andExpect(jsonPath("$.data[0].titulo").value("Colecao nova"))
                .andExpect(jsonPath("$.data[1].id").value(oldest.getId().toString()));
    }

    @Test
    @DisplayName("PUT /api/v1/materiais/{id} should update an available donor material")
    void shouldUpdateOwnedMaterial() throws Exception {
        Usuario owner = createUser("owner-update@example.com");
        Material material = createMaterial(owner, "Colecao original", StatusMaterial.DISPONIVEL, LocalDateTime.now().minusDays(1));

        mockMvc.perform(put("/v1/materiais/{id}", material.getId())
                        .header("Authorization", "Bearer " + tokenFor(owner))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Colecao revisada",
                                  "autor": "Ana Autora",
                                  "editora": "Editora Atualizada",
                                  "descricao": "Descricao revisada com mais contexto para o proximo estudante.",
                                  "disciplina": "PORTUGUES",
                                  "nivel_ensino": "MEDIO",
                                  "ano": 2,
                                  "sistema_ensino": "OBJETIVO",
                                  "estado_conservacao": "USADO",
                                  "data_publicacao": 2020
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.titulo").value("Colecao revisada"))
                .andExpect(jsonPath("$.data.autor").value("Ana Autora"))
                .andExpect(jsonPath("$.data.disciplina").value("PORTUGUES"))
                .andExpect(jsonPath("$.data.nivel_ensino").value("MEDIO"))
                .andExpect(jsonPath("$.data.sistema_ensino").value("OBJETIVO"))
                .andExpect(jsonPath("$.data.estado_conservacao").value("USADO"))
                .andExpect(jsonPath("$.data.data_publicacao").value(2020));

        assertThat(materialRepository.findById(material.getId()))
                .hasValueSatisfying(updated -> {
                    assertThat(updated.getTitulo()).isEqualTo("Colecao revisada");
                    assertThat(updated.getAutor()).isEqualTo("Ana Autora");
                    assertThat(updated.getDescricao()).contains("Descricao revisada");
                    assertThat(updated.getDisciplina()).isEqualTo(Disciplina.PORTUGUES);
                    assertThat(updated.getNivelEnsino()).isEqualTo(NivelEnsino.MEDIO);
                    assertThat(updated.getAno()).isEqualTo(2);
                    assertThat(updated.getSistemaEnsino()).isEqualTo(SistemaEnsino.OBJETIVO);
                    assertThat(updated.getEstadoConservacao()).isEqualTo(EstadoConservacao.USADO);
                    assertThat(updated.getDataPublicacao()).isEqualTo(2020);
                });
    }

    @Test
    @DisplayName("PUT /api/v1/materiais/{id} should reject updates from non owners")
    void shouldRejectForeignMaterialUpdate() throws Exception {
        Usuario owner = createUser("owner-foreign@example.com");
        Usuario intruder = createUser("intruder@example.com");
        Material material = createMaterial(owner, "Colecao protegida", StatusMaterial.DISPONIVEL, LocalDateTime.now());

        mockMvc.perform(put("/v1/materiais/{id}", material.getId())
                        .header("Authorization", "Bearer " + tokenFor(intruder))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Tentativa externa",
                                  "descricao": "Descricao suficiente para cumprir o contrato.",
                                  "disciplina": "MATEMATICA",
                                  "nivel_ensino": "FUNDAMENTAL",
                                  "ano": 7,
                                  "sistema_ensino": "ANGLO",
                                  "estado_conservacao": "BOM"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Apenas o doador do material pode alterar este cadastro"));
    }

    @Test
    @DisplayName("DELETE /api/v1/materiais/{id} should permanently remove an available donor material")
    void shouldDeleteOwnedMaterial() throws Exception {
        Usuario owner = createUser("owner-delete@example.com");
        Material material = createMaterial(owner, "Colecao excluivel", StatusMaterial.DISPONIVEL, LocalDateTime.now());

        mockMvc.perform(delete("/v1/materiais/{id}", material.getId())
                        .header("Authorization", "Bearer " + tokenFor(owner)))
                .andExpect(status().isNoContent());

        assertThat(materialRepository.findById(material.getId())).isEmpty();

        mockMvc.perform(get("/v1/materiais/me")
                        .header("Authorization", "Bearer " + tokenFor(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private Usuario createUser(String email) {
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .email(email)
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Owner User")
                .whatsapp("+5511991234567")
                .cidade("FLORIANOPOLIS")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .consentimentoIa(true)
                .role(Role.USER)
                .build());
    }

    private Material createMaterial(Usuario owner,
                                    String titulo,
                                    StatusMaterial status,
                                    LocalDateTime createdAt) {
        return materialRepository.saveAndFlush(Material.builder()
                .doador(owner)
                .titulo(titulo)
                .descricao("Descricao detalhada para " + titulo)
                .disciplina(Disciplina.MATEMATICA)
                .nivelEnsino(NivelEnsino.FUNDAMENTAL)
                .ano(7)
                .sistemaEnsino(SistemaEnsino.ANGLO)
                .estadoConservacao(EstadoConservacao.BOM)
                .status(status)
                .imagemUrl("/uploads/" + owner.getId() + "/" + titulo.replace(' ', '-').toLowerCase() + ".jpg")
                .cidade(owner.getCidade())
                .bairro(owner.getBairro())
                .dataPublicacao(2022)
                .criadoEm(createdAt)
                .atualizadoEm(createdAt)
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
