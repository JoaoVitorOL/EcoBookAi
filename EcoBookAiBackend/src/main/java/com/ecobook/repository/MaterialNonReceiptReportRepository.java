package com.ecobook.repository;

import com.ecobook.model.MaterialNonReceiptReport;
import com.ecobook.model.enums.NonReceiptReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaterialNonReceiptReportRepository extends JpaRepository<MaterialNonReceiptReport, UUID> {
    boolean existsBySolicitacaoIdAndStatus(UUID solicitacaoId, NonReceiptReportStatus status);

    @EntityGraph(attributePaths = {"material", "material.doador", "solicitacao", "estudante"})
    Page<MaterialNonReceiptReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"material", "material.doador", "solicitacao", "estudante"})
    Page<MaterialNonReceiptReport> findByStatusOrderByCreatedAtDesc(NonReceiptReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"material", "material.doador", "solicitacao", "estudante"})
    @Query("select report from MaterialNonReceiptReport report where report.id = :id")
    Optional<MaterialNonReceiptReport> findDetailedById(@Param("id") UUID id);
}
