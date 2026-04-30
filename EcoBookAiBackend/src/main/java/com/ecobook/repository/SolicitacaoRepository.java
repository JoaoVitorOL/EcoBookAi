package com.ecobook.repository;

import com.ecobook.model.Solicitacao;
import com.ecobook.model.enums.StatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitacaoRepository extends JpaRepository<Solicitacao, String> {
    List<Solicitacao> findByMaterialId(String materialId);
    List<Solicitacao> findByEstudanteId(String estudanteId);
    List<Solicitacao> findByStatus(StatusSolicitacao status);
    Optional<Solicitacao> findByMaterialIdAndEstudanteId(String materialId, String estudanteId);
    List<Solicitacao> findByStatusAndExpiresAtIsNotNull(StatusSolicitacao status);
}
