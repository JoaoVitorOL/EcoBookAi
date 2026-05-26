package com.ecobook.repository;

import com.ecobook.model.Solicitacao;
import com.ecobook.model.enums.StatusSolicitacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SolicitacaoRepository extends JpaRepository<Solicitacao, UUID> {
    List<Solicitacao> findByMaterialId(UUID materialId);
    List<Solicitacao> findByEstudanteId(UUID estudanteId);
    List<Solicitacao> findByStatus(StatusSolicitacao status);
    Optional<Solicitacao> findByMaterialIdAndEstudanteId(UUID materialId, UUID estudanteId);
    Optional<Solicitacao> findFirstByMaterialIdAndEstudanteIdAndStatusOrderByCriadoEmDesc(UUID materialId,
                                                                                           UUID estudanteId,
                                                                                           StatusSolicitacao status);
    List<Solicitacao> findByStatusAndExpiresAtIsNotNull(StatusSolicitacao status);
    List<Solicitacao> findByMaterialIdAndStatus(UUID materialId, StatusSolicitacao status);
    List<Solicitacao> findByEstudanteIdOrderByCriadoEmDesc(UUID estudanteId);
    List<Solicitacao> findByMaterialDoadorIdOrderByCriadoEmDesc(UUID doadorId);
    List<Solicitacao> findByMaterialDoadorIdAndStatusOrderByCriadoEmDesc(UUID doadorId, StatusSolicitacao status);
    boolean existsByMaterialIdAndEstudanteIdAndStatusIn(UUID materialId,
                                                        UUID estudanteId,
                                                        Collection<StatusSolicitacao> statuses);
    boolean existsByMaterialIdAndStatusAndIdNot(UUID materialId, StatusSolicitacao status, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Solicitacao s where s.id = :id")
    Optional<Solicitacao> findByIdForUpdate(UUID id);
}
