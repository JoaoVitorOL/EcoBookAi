package com.ecobook.repository;

import com.ecobook.model.Solicitacao;
import com.ecobook.model.enums.StatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SolicitacaoRepository extends JpaRepository<Solicitacao, UUID> {
    List<Solicitacao> findByMaterialId(UUID materialId);
    List<Solicitacao> findByEstudanteId(UUID estudanteId);
    List<Solicitacao> findByStatus(StatusSolicitacao status);
    Optional<Solicitacao> findByMaterialIdAndEstudanteId(UUID materialId, UUID estudanteId);
    List<Solicitacao> findByStatusAndExpiresAtIsNotNull(StatusSolicitacao status);
}
