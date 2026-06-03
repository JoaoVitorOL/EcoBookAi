package com.ecobook.scheduler;

import com.ecobook.BaseIntegrationTest;
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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationExpiryJobTest extends BaseIntegrationTest {

    private static final String SEEDED_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHiPpclJ3DgA6hfa/vw/jemjL5V6cO9e";

    @Autowired
    private ReservationExpiryJob reservationExpiryJob;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private SolicitacaoRepository solicitacaoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("reservation expiry job should release reserved materials whose approved request expired")
    void shouldExpireApprovedReservation() {
        Usuario donor = createUser("expiry-donor@example.com", "Doador");
        Usuario student = createUser("expiry-student@example.com", "Estudante");
        Material material = materialRepository.saveAndFlush(Material.builder()
                .doador(donor)
                .titulo("Colecao expirada")
                .descricao("Descricao da colecao expirada")
                .disciplina(Disciplina.MATEMATICA)
                .nivelEnsino(NivelEnsino.FUNDAMENTAL)
                .ano(8)
                .sistemaEnsino(SistemaEnsino.ANGLO)
                .estadoConservacao(EstadoConservacao.USADO)
                .status(StatusMaterial.RESERVADO)
                .cidade(donor.getCidade())
                .bairro(donor.getBairro())
                .dataPublicacao(2021)
                .build());

        Solicitacao request = Solicitacao.builder()
                .material(material)
                .estudante(student)
                .status(StatusSolicitacao.APROVADA)
                .aprovadoEm(LocalDateTime.now().minusDays(20))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        request.populateContatoDoador(donor);
        request = solicitacaoRepository.saveAndFlush(request);

        reservationExpiryJob.expireReservations();
        entityManager.flush();
        entityManager.clear();

        assertThat(materialRepository.findById(material.getId()))
                .hasValueSatisfying(saved -> assertThat(saved.getStatus()).isEqualTo(StatusMaterial.DISPONIVEL));
        assertThat(solicitacaoRepository.findById(request.getId()))
                .hasValueSatisfying(saved -> {
                    assertThat(saved.getStatus()).isEqualTo(StatusSolicitacao.CANCELADA);
                    assertThat(saved.getContatoDoador()).isNull();
                    assertThat(saved.getExpiresAt()).isNull();
                });
    }

    @Test
    @DisplayName("reservation expiry job should expire approvals that hit the exact expiry moment")
    void shouldExpireApprovalAtExactBoundaryMoment() {
        Usuario donor = createUser("expiry-boundary-donor@example.com", "Doador");
        Usuario student = createUser("expiry-boundary-student@example.com", "Estudante");
        Material material = materialRepository.saveAndFlush(Material.builder()
                .doador(donor)
                .titulo("Colecao no limite exato")
                .descricao("Descricao da colecao no limite exato de expiracao")
                .disciplina(Disciplina.MATEMATICA)
                .nivelEnsino(NivelEnsino.FUNDAMENTAL)
                .ano(7)
                .sistemaEnsino(SistemaEnsino.ANGLO)
                .estadoConservacao(EstadoConservacao.USADO)
                .status(StatusMaterial.RESERVADO)
                .cidade(donor.getCidade())
                .bairro(donor.getBairro())
                .dataPublicacao(2022)
                .build());

        LocalDateTime boundary = LocalDateTime.now();
        Solicitacao request = Solicitacao.builder()
                .material(material)
                .estudante(student)
                .status(StatusSolicitacao.APROVADA)
                .aprovadoEm(boundary.minusDays(14))
                .expiresAt(boundary)
                .build();
        request.populateContatoDoador(donor);
        request = solicitacaoRepository.saveAndFlush(request);

        reservationExpiryJob.expireReservations();
        entityManager.flush();
        entityManager.clear();

        assertThat(materialRepository.findById(material.getId()))
                .hasValueSatisfying(saved -> assertThat(saved.getStatus()).isEqualTo(StatusMaterial.DISPONIVEL));
        assertThat(solicitacaoRepository.findById(request.getId()))
                .hasValueSatisfying(saved -> assertThat(saved.getStatus()).isEqualTo(StatusSolicitacao.CANCELADA));
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
}
