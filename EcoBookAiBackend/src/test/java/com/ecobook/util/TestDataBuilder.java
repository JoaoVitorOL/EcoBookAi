package com.ecobook.util;

import com.ecobook.model.Usuario;
import com.ecobook.model.Material;
import com.ecobook.model.Solicitacao;
import com.ecobook.model.enums.*;
import java.util.UUID;

/**
 * Test data builder for creating test objects
 */
public class TestDataBuilder {

    public static Usuario createTestUsuario() {
        return Usuario.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .email("test@example.com")
                .nome("Test User")
                .whatsapp("+5548991234567")
                .cidade("SAO PAULO")
                .bairro("CENTRO")
                .perfilCompleto(true)
                .consentimentoIa(true)
                .role(Role.USER)
                .build();
    }

    public static Usuario createTestDonor() {
        return Usuario.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .email("donor@example.com")
                .nome("Donor User")
                .whatsapp("+5548999999999")
                .cidade("CURITIBA")
                .bairro("BATEL")
                .perfilCompleto(true)
                .role(Role.USER)
                .build();
    }

    public static Material createTestMaterial() {
        return Material.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000003"))
                .titulo("Matemática para Iniciantes")
                .descricao("Livro de matemática para ensino fundamental")
                .disciplina(Disciplina.MATEMATICA)
                .nivelEnsino(NivelEnsino.FUNDAMENTAL)
                .ano(5)
                .sistemaEnsino(SistemaEnsino.OBJETIVO)
                .estadoConservacao(EstadoConservacao.BOM)
                .status(StatusMaterial.DISPONIVEL)
                .cidade("SAO PAULO")
                .bairro("CENTRO")
                .dataPublicacao(2020)
                .build();
    }

    public static Solicitacao createTestSolicitacao() {
        return Solicitacao.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000004"))
                .status(StatusSolicitacao.PENDENTE)
                .build();
    }
}
