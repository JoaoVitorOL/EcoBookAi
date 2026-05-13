package com.ecobook.service;

import com.ecobook.dto.SolicitacaoDTO;
import com.ecobook.dto.SolicitacaoMaterialDTO;
import com.ecobook.dto.SolicitacaoStudentDTO;
import com.ecobook.model.Material;
import com.ecobook.model.Solicitacao;
import com.ecobook.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class SolicitacaoMapper {

    public SolicitacaoDTO toDto(Solicitacao solicitacao) {
        Material material = solicitacao.getMaterial();
        Usuario estudante = solicitacao.getEstudante();

        return SolicitacaoDTO.builder()
                .id(solicitacao.getId().toString())
                .materialId(material.getId().toString())
                .estudanteId(estudante.getId().toString())
                .status(solicitacao.getStatus().name())
                .contatoDoador(solicitacao.getContatoDoador())
                .criadoEm(solicitacao.getCriadoEm())
                .atualizadoEm(solicitacao.getAtualizadoEm())
                .aprovadoEm(solicitacao.getAprovadoEm())
                .expiresAt(solicitacao.getExpiresAt())
                .concluidoEm(solicitacao.getConcluidoEm())
                .material(SolicitacaoMaterialDTO.builder()
                        .id(material.getId().toString())
                        .titulo(material.getTitulo())
                        .descricao(material.getDescricao())
                        .imagemUrl(material.getImagemUrl())
                        .disciplina(material.getDisciplina().name())
                        .nivelEnsino(material.getNivelEnsino().name())
                        .ano(material.getAno())
                        .status(material.getStatus().name())
                        .cidade(material.getCidade())
                        .bairro(material.getBairro())
                        .doadorNome(material.getDoador().getNome())
                        .build())
                .estudante(SolicitacaoStudentDTO.builder()
                        .id(estudante.getId().toString())
                        .nome(estudante.getNome())
                        .cidade(estudante.getCidade())
                        .bairro(estudante.getBairro())
                        .build())
                .build();
    }
}
