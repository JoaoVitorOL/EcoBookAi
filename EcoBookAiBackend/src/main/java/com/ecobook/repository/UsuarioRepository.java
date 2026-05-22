package com.ecobook.repository;

import com.ecobook.model.Usuario;
import com.ecobook.model.enums.NonReceiptReportStatus;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.model.enums.StatusSolicitacao;
import com.ecobook.repository.projection.AdminUserMetricsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Page<Usuario> findAllByOrderByCriadoEmDesc(Pageable pageable);

    @Query("""
            select
                u.id as userId,
                (select count(m) from Material m where m.doador.id = u.id) as materialsCount,
                (select count(m) from Material m where m.doador.id = u.id and m.status = :donatedStatus) as donatedMaterialsCount,
                (select count(s) from Solicitacao s where s.estudante.id = u.id) as requestsCount,
                (select count(s) from Solicitacao s where s.estudante.id = u.id and s.status = :completedStatus) as completedRequestsCount,
                (select count(r) from MaterialNonReceiptReport r where r.estudante.id = u.id and r.status = :openReportStatus) as openReportsCount
            from Usuario u
            where u.id in :userIds
            """)
    List<AdminUserMetricsProjection> findAdminMetricsByIds(@Param("userIds") Collection<UUID> userIds,
                                                           @Param("donatedStatus") StatusMaterial donatedStatus,
                                                           @Param("completedStatus") StatusSolicitacao completedStatus,
                                                           @Param("openReportStatus") NonReceiptReportStatus openReportStatus);
}
