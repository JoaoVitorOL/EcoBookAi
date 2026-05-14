package com.ecobook.service;

import com.ecobook.dto.MaterialDTO;
import com.ecobook.dto.MaterialDonorDTO;
import com.ecobook.model.Material;
import org.springframework.stereotype.Component;

@Component
public class MaterialMapper {

    public MaterialDTO toDto(Material material) {
        return MaterialDTO.builder()
                .id(material.getId().toString())
                .titulo(material.getTitulo())
                .autor(material.getAutor())
                .editora(material.getEditora())
                .descricao(material.getDescricao())
                .disciplina(material.getDisciplina().name())
                .nivelEnsino(material.getNivelEnsino().name())
                .ano(material.getAno())
                .sistemaEnsino(material.getSistemaEnsino().name())
                .estadoConservacao(material.getEstadoConservacao().name())
                .status(material.getStatus().name())
                .imagemUrl(material.getImagemUrl())
                .imagemVersoUrl(material.getImagemVersoUrl())
                .uploadId(material.getUploadId())
                .doador(MaterialDonorDTO.builder()
                        .id(material.getDoador().getId().toString())
                        .nome(material.getDoador().getNome())
                        .whatsapp(material.getDoador().getWhatsapp())
                        .cidade(material.getDoador().getCidade())
                        .bairro(material.getDoador().getBairro())
                        .build())
                .cidade(material.getCidade())
                .bairro(material.getBairro())
                .dataPublicacao(material.getDataPublicacao())
                .statusIa(material.getStatusIa() != null ? material.getStatusIa().name() : null)
                .confiancaIa(material.getConfiancaIa() != null ? material.getConfiancaIa().doubleValue() : null)
                .criadoEm(material.getCriadoEm())
                .atualizadoEm(material.getAtualizadoEm())
                .build();
    }
}
