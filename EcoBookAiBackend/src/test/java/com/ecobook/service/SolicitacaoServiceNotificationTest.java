package com.ecobook.service;

import com.ecobook.dto.notification.NotificationType;
import com.ecobook.event.NotificationRequestedEvent;
import com.ecobook.model.Material;
import com.ecobook.model.Solicitacao;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.model.enums.StatusSolicitacao;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.SolicitacaoRepository;
import com.ecobook.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitacaoServiceNotificationTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private SolicitacaoRepository solicitacaoRepository;

    @Mock
    private SolicitacaoMapper solicitacaoMapper;

    @Mock
    private MaterialStateValidator materialStateValidator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final NotificationPayloadFactory notificationPayloadFactory = new NotificationPayloadFactory();

    private SolicitacaoService solicitacaoService;

    @BeforeEach
    void setUp() {
        solicitacaoService = new SolicitacaoService(
                usuarioRepository,
                materialRepository,
                solicitacaoRepository,
                solicitacaoMapper,
                materialStateValidator,
                notificationPayloadFactory,
                eventPublisher
        );
    }

    @Test
    @DisplayName("createRequest should notify the donor that a new request was received")
    void createRequestShouldNotifyDonor() {
        Usuario doador = usuario("teste2@gmail.com", "Teste Dois");
        Usuario estudante = usuario("teste3@gmail.com", "Teste Tres");
        Material material = material(doador, "Colecao Anglo");

        Solicitacao persisted = Solicitacao.builder()
                .id(UUID.randomUUID())
                .material(material)
                .estudante(estudante)
                .status(StatusSolicitacao.PENDENTE)
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("teste3@gmail.com")).thenReturn(Optional.of(estudante));
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(solicitacaoRepository.existsByMaterialIdAndEstudanteIdAndStatusIn(
                eq(material.getId()),
                eq(estudante.getId()),
                any()
        )).thenReturn(false);
        when(solicitacaoRepository.save(any(Solicitacao.class))).thenReturn(persisted);

        solicitacaoService.createRequest("teste3@gmail.com", material.getId().toString());

        ArgumentCaptor<NotificationRequestedEvent> eventCaptor = ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        NotificationRequestedEvent event = eventCaptor.getValue();
        assertThat(event.recipientUserId()).isEqualTo(doador.getId());
        assertThat(event.payload().getType()).isEqualTo(NotificationType.SOLICITACAO_RECEBIDA);
        assertThat(event.payload().getRequestId()).isEqualTo(persisted.getId().toString());
        assertThat(event.payload().getMaterialId()).isEqualTo(material.getId().toString());
        assertThat(event.payload().getTitle()).isEqualTo("Novo pedido recebido");
    }

    @Test
    @DisplayName("approveRequest should notify the requesting student that the request was approved")
    void approveRequestShouldNotifyRequestingStudent() {
        Usuario doador = usuario("teste2@gmail.com", "Teste Dois");
        Usuario estudante = usuario("teste3@gmail.com", "Teste Tres");
        Material material = material(doador, "Colecao Anglo");
        material.setStatus(StatusMaterial.DISPONIVEL);

        Solicitacao solicitacao = Solicitacao.builder()
                .id(UUID.randomUUID())
                .material(material)
                .estudante(estudante)
                .status(StatusSolicitacao.PENDENTE)
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("teste2@gmail.com")).thenReturn(Optional.of(doador));
        when(solicitacaoRepository.findByIdForUpdate(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
        when(materialRepository.findByIdForUpdate(material.getId())).thenReturn(Optional.of(material));
        when(solicitacaoRepository.existsByMaterialIdAndStatusAndIdNot(
                material.getId(),
                StatusSolicitacao.APROVADA,
                solicitacao.getId()
        )).thenReturn(false);
        when(solicitacaoRepository.findByMaterialIdAndStatus(material.getId(), StatusSolicitacao.PENDENTE))
                .thenReturn(List.of(solicitacao));
        when(materialRepository.save(material)).thenReturn(material);
        when(solicitacaoRepository.save(solicitacao)).thenReturn(solicitacao);
        doNothing().when(materialStateValidator).requireAvailable(any(), any());
        doNothing().when(materialStateValidator).validateTransition(any(), any());

        solicitacaoService.approveRequest("teste2@gmail.com", solicitacao.getId().toString());

        ArgumentCaptor<NotificationRequestedEvent> eventCaptor = ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        NotificationRequestedEvent event = eventCaptor.getValue();
        assertThat(event.recipientUserId()).isEqualTo(estudante.getId());
        assertThat(event.payload().getType()).isEqualTo(NotificationType.SOLICITACAO_APROVADA);
        assertThat(event.payload().getRequestId()).isEqualTo(solicitacao.getId().toString());
        assertThat(event.payload().getMaterialId()).isEqualTo(material.getId().toString());
        assertThat(event.payload().getTitle()).isEqualTo("Solicitacao aprovada");
    }

    private Usuario usuario(String email, String nome) {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email(email)
                .nome(nome)
                .whatsapp("+5511999999999")
                .cpf("52998224725")
                .cidade("Rio de Janeiro")
                .bairro("Centro")
                .perfilCompleto(true)
                .consentimentoIa(true)
                .build();
    }

    private Material material(Usuario doador, String titulo) {
        return Material.builder()
                .id(UUID.randomUUID())
                .doador(doador)
                .titulo(titulo)
                .descricao("Livro de apoio escolar")
                .cidade("Rio de Janeiro")
                .bairro("Centro")
                .status(StatusMaterial.DISPONIVEL)
                .build();
    }
}
