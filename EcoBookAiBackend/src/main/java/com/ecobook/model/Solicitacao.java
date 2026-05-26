package com.ecobook.model;

import com.ecobook.model.enums.StatusSolicitacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
    name = "solicitacao",
    indexes = {
        @Index(name = "idx_solicitacao_material_id", columnList = "material_id"),
        @Index(name = "idx_solicitacao_estudante_id", columnList = "estudante_id"),
        @Index(name = "idx_solicitacao_status", columnList = "status"),
        @Index(name = "idx_solicitacao_expires_at", columnList = "expires_at")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"material", "estudante"})
@SQLRestriction("deleted_at IS NULL")
public class Solicitacao extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Material material;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estudante_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Usuario estudante;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "status_solicitacao_enum")
    @Builder.Default
    private StatusSolicitacao status = StatusSolicitacao.PENDENTE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, String> contatoDoador;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    @Column
    private LocalDateTime aprovadoEm;

    @Column
    private LocalDateTime expiresAt;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    @PreUpdate
    protected void onUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    /**
     * Set contact information for the donor
     * Only set when status changes to APROVADA
     */
    public void populateContatoDoador(Usuario doador) {
        if (this.status == StatusSolicitacao.APROVADA) {
            this.contatoDoador = new HashMap<>();
            this.contatoDoador.put("nome", doador.getNome());
            this.contatoDoador.put("whatsapp", doador.getWhatsapp());
        }
    }

    /**
     * Checks whether the approval window already elapsed.
     *
     * @return true when the request reached or passed its expiry instant
     */
    public boolean hasExpired() {
        return hasExpired(LocalDateTime.now());
    }

    /**
     * Checks whether the approval window already elapsed for a reference instant.
     *
     * @param referenceTime instant used for the comparison
     * @return true when the request reached or passed its expiry instant
     */
    public boolean hasExpired(LocalDateTime referenceTime) {
        return this.expiresAt != null
                && referenceTime != null
                && !referenceTime.isBefore(this.expiresAt);
    }

    /**
     * Check if request is in active/pending state
     */
    public boolean isActive() {
        return this.status == StatusSolicitacao.PENDENTE ||
                this.status == StatusSolicitacao.APROVADA;
    }
}
