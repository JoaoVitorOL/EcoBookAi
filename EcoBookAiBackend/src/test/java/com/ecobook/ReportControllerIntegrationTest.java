package com.ecobook;

import com.ecobook.model.Material;
import com.ecobook.model.MaterialNonReceiptReport;
import com.ecobook.model.Solicitacao;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Disciplina;
import com.ecobook.model.enums.EstadoConservacao;
import com.ecobook.model.enums.Role;
import com.ecobook.model.enums.SistemaEnsino;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.model.enums.StatusSolicitacao;
import com.ecobook.model.enums.NivelEnsino;
import com.ecobook.repository.MaterialNonReceiptReportRepository;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.SolicitacaoRepository;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportControllerIntegrationTest extends BaseIntegrationTest {

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
    private MaterialNonReceiptReportRepository materialNonReceiptReportRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("POST /api/v1/materiais/{id}/nao-recebido should create an open report for the completed student")
    void shouldCreateNonReceiptReport() throws Exception {
        Usuario donor = createUser("donor-report@example.com", "Doador");
        Usuario student = createUser("student-report@example.com", "Estudante");
        Material material = createDonatedMaterial(donor, "Colecao reportavel");
        Solicitacao request = createCompletedRequest(material, student);

        mockMvc.perform(post("/v1/materiais/{id}/nao-recebido", material.getId())
                        .header("Authorization", "Bearer " + tokenFor(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Combinei a retirada, mas o material não chegou."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.material_id").value(material.getId().toString()))
                .andExpect(jsonPath("$.data.solicitacao_id").value(request.getId().toString()))
                .andExpect(jsonPath("$.data.estudante_id").value(student.getId().toString()))
                .andExpect(jsonPath("$.data.reason").value("Combinei a retirada, mas o material não chegou."));

        assertThat(materialNonReceiptReportRepository.findAll())
                .singleElement()
                .satisfies(report -> {
                    assertThat(report.getStatus().name()).isEqualTo("OPEN");
                    assertThat(report.getSolicitacao().getId()).isEqualTo(request.getId());
                });
    }

    @Test
    @DisplayName("POST /api/v1/materiais/{id}/nao-recebido should reject a duplicate open report")
    void shouldRejectDuplicateOpenReport() throws Exception {
        Usuario donor = createUser("donor-report-duplicate@example.com", "Doador");
        Usuario student = createUser("student-report-duplicate@example.com", "Estudante");
        Material material = createDonatedMaterial(donor, "Colecao duplicada");
        Solicitacao request = createCompletedRequest(material, student);

        materialNonReceiptReportRepository.saveAndFlush(MaterialNonReceiptReport.builder()
                .material(material)
                .solicitacao(request)
                .estudante(student)
                .reason("Primeiro reporte")
                .build());

        mockMvc.perform(post("/v1/materiais/{id}/nao-recebido", material.getId())
                        .header("Authorization", "Bearer " + tokenFor(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Tentativa repetida"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Já existe um reporte aberto para este material"));
    }

    @Test
    @DisplayName("POST /api/v1/materiais/{id}/nao-recebido should reject non participants")
    void shouldRejectUsersWithoutCompletedStudentRequest() throws Exception {
        Usuario donor = createUser("donor-report-forbidden@example.com", "Doador");
        Usuario student = createUser("student-report-forbidden@example.com", "Estudante");
        Usuario outsider = createUser("outsider-report@example.com", "Intruso");
        Material material = createDonatedMaterial(donor, "Colecao protegida");
        createCompletedRequest(material, student);

        mockMvc.perform(post("/v1/materiais/{id}/nao-recebido", material.getId())
                        .header("Authorization", "Bearer " + tokenFor(outsider))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Nao recebi"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/materiais/{id}/nao-recebido should require a donated material")
    void shouldRequireDonatedMaterial() throws Exception {
        Usuario donor = createUser("donor-report-pending@example.com", "Doador");
        Usuario student = createUser("student-report-pending@example.com", "Estudante");
        Material material = createAvailableMaterial(donor, "Colecao ainda disponivel");
        solicitacaoRepository.saveAndFlush(Solicitacao.builder()
                .material(material)
                .estudante(student)
                .status(StatusSolicitacao.PENDENTE)
                .build());

        mockMvc.perform(post("/v1/materiais/{id}/nao-recebido", material.getId())
                        .header("Authorization", "Bearer " + tokenFor(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Somente materiais marcados como doados podem ser reportados"));
    }

    private Usuario createUser(String email, String nome) {
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .email(email)
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome(nome)
                .whatsapp("+5511991234567")
                .cpf("52998224725")
                .cidade("FLORIANOPOLIS")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .consentimentoIa(true)
                .role(Role.USER)
                .build());
    }

    private Material createAvailableMaterial(Usuario donor, String title) {
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

    private Material createDonatedMaterial(Usuario donor, String title) {
        Material material = createAvailableMaterial(donor, title);
        material.setStatus(StatusMaterial.DOADO);
        material.setDoadoEm(LocalDateTime.now().minusDays(1));
        return materialRepository.saveAndFlush(material);
    }

    private Solicitacao createCompletedRequest(Material material, Usuario student) {
        return solicitacaoRepository.saveAndFlush(Solicitacao.builder()
                .material(material)
                .estudante(student)
                .status(StatusSolicitacao.CONCLUIDA)
                .aprovadoEm(LocalDateTime.now().minusDays(2))
                .concluidoEm(LocalDateTime.now().minusDays(1))
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
