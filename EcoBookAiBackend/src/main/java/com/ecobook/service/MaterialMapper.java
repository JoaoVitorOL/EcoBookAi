package com.ecobook.service;

import com.ecobook.dto.MaterialDTO;
import com.ecobook.dto.MaterialDonorDTO;
import com.ecobook.model.Material;
import org.springframework.stereotype.Component;

@Component
public class MaterialMapper {

    /**
     * Maps a material entity into the API DTO representation.
     * @param material material entity to map or inspect
     * @return mapped DTO representation
     */
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
                .necessidadeAcademica(material.getNecessidadeAcademica().name())
                .status(material.getStatus().name())
                .imagemUrl(resolvePrimaryImageUrl(material))
                .imagemVersoUrl(resolveBackImageUrl(material))
                .uploadId(material.getUploadId())
                .doador(MaterialDonorDTO.builder()
                        .id(material.getDoador() == null || material.getDoador().getId() == null ? null : material.getDoador().getId().toString())
                        .nome(material.getDoador() == null ? "Conta removida" : material.getDoador().getNome())
                        .whatsapp(material.getDoador() == null ? null : material.getDoador().getWhatsapp())
                        .cidade(material.getDoador() == null ? null : material.getDoador().getCidade())
                        .bairro(material.getDoador() == null ? null : material.getDoador().getBairro())
                        .fotoPerfilUrl(UserProfilePhotoPaths.resolveUrl(material.getDoador()))
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

    private String resolvePrimaryImageUrl(Material material) {
        if (material.getUploadTrackingId() != null) {
            return "/api/v1/images/" + material.getUploadTrackingId();
        }
        return material.getImagemUrl();
    }

    private String resolveBackImageUrl(Material material) {
        if (material.getUploadTrackingId() != null) {
            return "/api/v1/images/" + material.getUploadTrackingId() + "?side=back";
        }
        return material.getImagemVersoUrl();
    }
}
