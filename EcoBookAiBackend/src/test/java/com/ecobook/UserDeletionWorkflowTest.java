package com.ecobook;

import com.ecobook.model.Material;
import com.ecobook.model.Solicitacao;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Disciplina;
import com.ecobook.model.enums.EstadoConservacao;
import com.ecobook.model.enums.NivelEnsino;
import com.ecobook.model.enums.Role;
import com.ecobook.model.enums.SistemaEnsino;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.model.enums.StatusSolicitacao;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.SolicitacaoRepository;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserDeletionWorkflowTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private SolicitacaoRepository solicitacaoRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("POST /api/v1/usuarios/delete should reopen a reserved material when the approved student deletes the account")
    void shouldReopenReservedMaterialWhenApprovedStudentDeletesAccount() throws Exception {
        Usuario donor = createUser("deletion-donor@example.com", "Doador", "safe-password-123");
        Usuario student = createUser("deletion-student@example.com", "Estudante", "safe-password-123");
        Material material = materialRepository.saveAndFlush(Material.builder()
                .doador(donor)
                .titulo("Colecao reservada para exclusao")
                .descricao("Descricao detalhada da colecao reservada para exclusao")
                .disciplina(Disciplina.MATEMATICA)
                .nivelEnsino(NivelEnsino.FUNDAMENTAL)
                .ano(8)
                .sistemaEnsino(SistemaEnsino.ANGLO)
                .estadoConservacao(EstadoConservacao.BOM)
                .status(StatusMaterial.RESERVADO)
                .cidade(donor.getCidade())
                .bairro(donor.getBairro())
                .dataPublicacao(2023)
                .build());

        Solicitacao request = Solicitacao.builder()
                .material(material)
                .estudante(student)
                .status(StatusSolicitacao.APROVADA)
                .aprovadoEm(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(13))
                .build();
        request.populateContatoDoador(donor);
        request = solicitacaoRepository.saveAndFlush(request);

        String studentToken = tokenFor(student);

        mockMvc.perform(post("/v1/usuarios/delete")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "safe-password-123",
                                  "reason": "Fluxo de remocao de conta em teste"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value(student.getId().toString()))
                .andExpect(jsonPath("$.data.deleted_at").isNotEmpty());

        assertThat(materialRepository.findById(material.getId()))
                .hasValueSatisfying(saved -> {
                    assertThat(saved.getStatus()).isEqualTo(StatusMaterial.DISPONIVEL);
                    assertThat(saved.getDoadoEm()).isNull();
                });
        assertThat(solicitacaoRepository.findById(request.getId()))
                .hasValueSatisfying(saved -> {
                    assertThat(saved.getStatus()).isEqualTo(StatusSolicitacao.CANCELADA);
                    assertThat(saved.getContatoDoador()).isNull();
                    assertThat(saved.getDeletedAt()).isNotNull();
                });
        assertThat(usuarioRepository.findById(student.getId()))
                .hasValueSatisfying(saved -> {
                    assertThat(saved.getEmail()).startsWith("deleted+" + student.getId());
                    assertThat(saved.isPerfilCompleto()).isFalse();
                    assertThat(saved.getWhatsapp()).isNull();
                });

        mockMvc.perform(get("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    private Usuario createUser(String email, String nome, String rawPassword) {
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .nome(nome)
                .whatsapp("+5511991234567")
                .cidade("FLORIANOPOLIS")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .consentimentoIa(true)
                .role(Role.USER)
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
