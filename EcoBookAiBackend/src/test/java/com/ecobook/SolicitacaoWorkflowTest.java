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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SolicitacaoWorkflowTest extends BaseIntegrationTest {

    private static final String SEEDED_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHiPpclJ3DgA6hfa/vw/jemjL5V6cO9e";

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

    @Test
    @DisplayName("POST /api/v1/materiais/{id}/solicitacoes should create a pending request")
    void shouldCreatePendingRequest() throws Exception {
        Usuario donor = createUser("donor-create-request@example.com", "Doador");
        Usuario student = createUser("student-create-request@example.com", "Estudante");
        Material material = createMaterial(donor, "Colecao para pedido");

        mockMvc.perform(post("/v1/materiais/{id}/solicitacoes", material.getId())
                        .header("Authorization", "Bearer " + tokenFor(student)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDENTE"))
                .andExpect(jsonPath("$.data.material_id").value(material.getId().toString()))
                .andExpect(jsonPath("$.data.estudante_id").value(student.getId().toString()))
                .andExpect(jsonPath("$.data.contato_doador").doesNotExist());

        assertThat(solicitacaoRepository.findByMaterialId(material.getId()))
                .singleElement()
                .satisfies(request -> {
                    assertThat(request.getStatus()).isEqualTo(StatusSolicitacao.PENDENTE);
                    assertThat(request.getEstudante().getId()).isEqualTo(student.getId());
                });
    }

    @Test
    @DisplayName("POST /api/v1/materiais/{id}/solicitacoes should reject self requests")
    void shouldRejectSelfRequest() throws Exception {
        Usuario donor = createUser("donor-self-request@example.com", "Doador");
        Material material = createMaterial(donor, "Colecao protegida do proprio doador");

        mockMvc.perform(post("/v1/materiais/{id}/solicitacoes", material.getId())
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nao e possivel solicitar o proprio material"));
    }

    @Test
    @DisplayName("approval should reserve the material, auto-reject competing requests and allow cancellation")
    void shouldApproveAndCancelRequestWorkflow() throws Exception {
        Usuario donor = createUser("donor-workflow@example.com", "Doador");
        Usuario firstStudent = createUser("student-first@example.com", "Primeiro");
        Usuario secondStudent = createUser("student-second@example.com", "Segundo");
        Material material = createMaterial(donor, "Colecao concorrida");

        Solicitacao firstRequest = createPendingRequest(material, firstStudent);
        Solicitacao secondRequest = createPendingRequest(material, secondStudent);

        mockMvc.perform(patch("/v1/solicitacoes/{id}/aprovar", firstRequest.getId())
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APROVADA"))
                .andExpect(jsonPath("$.data.contato_doador.nome").value(donor.getNome()))
                .andExpect(jsonPath("$.data.material.status").value("RESERVADO"));

        assertThat(materialRepository.findById(material.getId()))
                .hasValueSatisfying(saved -> assertThat(saved.getStatus()).isEqualTo(StatusMaterial.RESERVADO));
        assertThat(solicitacaoRepository.findById(secondRequest.getId()))
                .hasValueSatisfying(saved -> assertThat(saved.getStatus()).isEqualTo(StatusSolicitacao.RECUSADA));

        mockMvc.perform(post("/v1/materiais/{id}/solicitacoes", material.getId())
                        .header("Authorization", "Bearer " + tokenFor(secondStudent)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("O material ja possui uma solicitacao aprovada"));

        mockMvc.perform(patch("/v1/solicitacoes/{id}/cancelar", firstRequest.getId())
                        .header("Authorization", "Bearer " + tokenFor(firstStudent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELADA"));

        assertThat(materialRepository.findById(material.getId()))
                .hasValueSatisfying(saved -> assertThat(saved.getStatus()).isEqualTo(StatusMaterial.DISPONIVEL));
    }

    @Test
    @DisplayName("completion should mark both request and material as finished")
    void shouldCompleteDonation() throws Exception {
        Usuario donor = createUser("donor-complete@example.com", "Doador");
        Usuario student = createUser("student-complete@example.com", "Estudante");
        Material material = createMaterial(donor, "Colecao doada");
        Solicitacao request = createApprovedRequest(material, student, donor);

        mockMvc.perform(patch("/v1/solicitacoes/{id}/concluir", request.getId())
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.data.concluido_em").isNotEmpty())
                .andExpect(jsonPath("$.data.contato_doador").doesNotExist())
                .andExpect(jsonPath("$.data.expires_at").doesNotExist());

        assertThat(materialRepository.findById(material.getId()))
                .hasValueSatisfying(saved -> {
                    assertThat(saved.getStatus()).isEqualTo(StatusMaterial.DOADO);
                    assertThat(saved.getDoadoEm()).isNotNull();
                });
        assertThat(solicitacaoRepository.findById(request.getId()))
                .hasValueSatisfying(saved -> {
                    assertThat(saved.getStatus()).isEqualTo(StatusSolicitacao.CONCLUIDA);
                    assertThat(saved.getConcluidoEm()).isNotNull();
                    assertThat(saved.getContatoDoador()).isNull();
                    assertThat(saved.getExpiresAt()).isNull();
                });
    }

    @Test
    @DisplayName("donor should be able to revoke a previous approval and reopen the material")
    void shouldAllowDonorToRevokeApproval() throws Exception {
        Usuario donor = createUser("donor-revoke@example.com", "Doador");
        Usuario student = createUser("student-revoke@example.com", "Estudante");
        Material material = createMaterial(donor, "Colecao revogavel");
        Solicitacao request = createApprovedRequest(material, student, donor);

        mockMvc.perform(patch("/v1/solicitacoes/{id}/cancelar", request.getId())
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELADA"))
                .andExpect(jsonPath("$.data.contato_doador").doesNotExist());

        assertThat(materialRepository.findById(material.getId()))
                .hasValueSatisfying(saved -> {
                    assertThat(saved.getStatus()).isEqualTo(StatusMaterial.DISPONIVEL);
                    assertThat(saved.getDoadoEm()).isNull();
                });
        assertThat(solicitacaoRepository.findById(request.getId()))
                .hasValueSatisfying(saved -> {
                    assertThat(saved.getStatus()).isEqualTo(StatusSolicitacao.CANCELADA);
                    assertThat(saved.getContatoDoador()).isNull();
                    assertThat(saved.getExpiresAt()).isNull();
                });
    }

    @Test
    @DisplayName("listing endpoints should separate student, donor pending and donor approved requests")
    void shouldListRequestsByAudience() throws Exception {
        Usuario donor = createUser("donor-list-requests@example.com", "Doador");
        Usuario pendingStudent = createUser("student-pending-list@example.com", "Pendente");
        Usuario approvedStudent = createUser("student-approved-list@example.com", "Aprovado");
        Material pendingMaterial = createMaterial(donor, "Colecao pendente");
        Material approvedMaterial = createMaterial(donor, "Colecao aprovada");

        Solicitacao pendingRequest = createPendingRequest(pendingMaterial, pendingStudent);
        Solicitacao approvedRequest = createApprovedRequest(approvedMaterial, approvedStudent, donor);

        mockMvc.perform(get("/v1/solicitacoes/minhas")
                        .header("Authorization", "Bearer " + tokenFor(approvedStudent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(approvedRequest.getId().toString()))
                .andExpect(jsonPath("$.data[0].material.titulo").value("Colecao aprovada"));

        mockMvc.perform(get("/v1/solicitacoes/pendentes")
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(pendingRequest.getId().toString()));

        mockMvc.perform(get("/v1/solicitacoes/aprovadas")
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(approvedRequest.getId().toString()));
    }

    @Test
    @DisplayName("GET /api/v1/solicitacoes/minhas should reject invalid status filters with HTTP 400")
    void shouldRejectInvalidRequestStatusFilter() throws Exception {
        Usuario student = createUser("student-invalid-filter@example.com", "Estudante");

        mockMvc.perform(get("/v1/solicitacoes/minhas")
                        .param("status", "EM_ANALISE")
                        .header("Authorization", "Bearer " + tokenFor(student)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O filtro de status informado e invalido"))
                .andExpect(jsonPath("$.field_errors.status").value(
                        "Use um dos valores: PENDENTE, APROVADA, RECUSADA, CANCELADA, CONCLUIDA"
                ));
    }

    private Usuario createUser(String email, String nome) {
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .email(email)
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome(nome)
                .whatsapp("+5511991234567")
                .cidade("FLORIANOPOLIS")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .consentimentoIa(true)
                .role(Role.USER)
                .build());
    }

    private Material createMaterial(Usuario donor, String title) {
        return materialRepository.saveAndFlush(Material.builder()
                .doador(donor)
                .titulo(title)
                .descricao("Descricao completa para " + title)
                .disciplina(Disciplina.MATEMATICA)
                .nivelEnsino(NivelEnsino.FUNDAMENTAL)
                .ano(6)
                .sistemaEnsino(SistemaEnsino.ANGLO)
                .estadoConservacao(EstadoConservacao.BOM)
                .status(StatusMaterial.DISPONIVEL)
                .imagemUrl("/api/uploads/" + donor.getId() + "/" + title.replace(' ', '-').toLowerCase() + ".jpg")
                .cidade(donor.getCidade())
                .bairro(donor.getBairro())
                .dataPublicacao(2024)
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build());
    }

    private Solicitacao createPendingRequest(Material material, Usuario student) {
        return solicitacaoRepository.saveAndFlush(Solicitacao.builder()
                .material(material)
                .estudante(student)
                .status(StatusSolicitacao.PENDENTE)
                .build());
    }

    private Solicitacao createApprovedRequest(Material material, Usuario student, Usuario donor) {
        material.setStatus(StatusMaterial.RESERVADO);
        materialRepository.saveAndFlush(material);

        Solicitacao request = Solicitacao.builder()
                .material(material)
                .estudante(student)
                .status(StatusSolicitacao.APROVADA)
                .aprovadoEm(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(13))
                .build();
        request.populateContatoDoador(donor);
        return solicitacaoRepository.saveAndFlush(request);
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
