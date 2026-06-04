package com.ecobook.repository;

import com.ecobook.model.Material;
import com.ecobook.model.enums.StatusMaterial;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaterialRepository extends JpaRepository<Material, UUID>, JpaSpecificationExecutor<Material> {
    List<Material> findByDoadorId(UUID doadorId);
    List<Material> findByDoadorIdOrderByCriadoEmDesc(UUID doadorId);
    List<Material> findByDoadorIdAndStatusInOrderByCriadoEmDesc(UUID doadorId, Collection<StatusMaterial> statuses);
    List<Material> findByDoadorIdAndUploadTrackingIdIsNotNullOrderByCriadoEmDesc(UUID doadorId);
    List<Material> findByStatus(StatusMaterial status);
    List<Material> findByCidadeAndBairro(String cidade, String bairro);
    Optional<Material> findByUploadTrackingId(UUID uploadTrackingId);
    Page<Material> findAllByOrderByCriadoEmDesc(Pageable pageable);
    Page<Material> findByStatusOrderByCriadoEmDesc(StatusMaterial status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Material m where m.id = :id")
    Optional<Material> findByIdForUpdate(UUID id);
}
