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

    /**
     * Maps a request entity into the API DTO representation.
     * @param solicitacao request entity to map or inspect
     * @return mapped DTO representation
     */
    public SolicitacaoDTO toDto(Solicitacao solicitacao) {
        Material material = solicitacao.getMaterial();
        Usuario estudante = solicitacao.getEstudante();
        String secureImageUrl = material == null
                ? null
                : material.getUploadTrackingId() == null
                ? material.getImagemUrl()
                : "/api/v1/images/" + material.getUploadTrackingId();

        return SolicitacaoDTO.builder()
                .id(solicitacao.getId().toString())
                .materialId(material == null || material.getId() == null ? null : material.getId().toString())
                .estudanteId(estudante == null || estudante.getId() == null ? null : estudante.getId().toString())
                .status(solicitacao.getStatus().name())
                .contatoDoador(solicitacao.getContatoDoador())
                .criadoEm(solicitacao.getCriadoEm())
                .atualizadoEm(solicitacao.getAtualizadoEm())
                .aprovadoEm(solicitacao.getAprovadoEm())
                .expiresAt(solicitacao.getExpiresAt())
                .concluidoEm(solicitacao.getConcluidoEm())
                .material(SolicitacaoMaterialDTO.builder()
                        .id(material == null || material.getId() == null ? null : material.getId().toString())
                        .titulo(material == null ? null : material.getTitulo())
                        .descricao(material == null ? null : material.getDescricao())
                        .imagemUrl(secureImageUrl)
                        .disciplina(material == null || material.getDisciplina() == null ? null : material.getDisciplina().name())
                        .nivelEnsino(material == null || material.getNivelEnsino() == null ? null : material.getNivelEnsino().name())
                        .ano(material == null ? null : material.getAno())
                        .status(material == null || material.getStatus() == null ? null : material.getStatus().name())
                        .cidade(material == null ? null : material.getCidade())
                        .bairro(material == null ? null : material.getBairro())
                        .doadorNome(material == null || material.getDoador() == null ? "Conta removida" : material.getDoador().getNome())
                        .build())
                .estudante(SolicitacaoStudentDTO.builder()
                        .id(estudante == null || estudante.getId() == null ? null : estudante.getId().toString())
                        .nome(estudante == null ? "Conta removida" : estudante.getNome())
                        .cidade(estudante == null ? null : estudante.getCidade())
                        .bairro(estudante == null ? null : estudante.getBairro())
                        .build())
                .build();
    }
}
