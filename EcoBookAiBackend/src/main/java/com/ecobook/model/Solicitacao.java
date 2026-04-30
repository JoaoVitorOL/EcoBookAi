package com.ecobook.model;

import com.ecobook.model.enums.StatusSolicitacao;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
public class Solicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private String id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "estudante_id", nullable = false)
    private Usuario estudante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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

    @PreUpdate
    protected void onUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    /**
     * Set contact information for the donor
     * Only set when status changes to APROVADA
     */
    public void setContatoDoador(Usuario doador) {
        if (this.status == StatusSolicitacao.APROVADA) {
            this.contatoDoador = new HashMap<>();
            this.contatoDoador.put("nome", doador.getNome());
            this.contatoDoador.put("whatsapp", doador.getWhatsapp());
        }
    }

    /**
     * Check if request has expired
     */
    public boolean hasExpired() {
        return this.expiresAt != null && LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * Check if request is in active/pending state
     */
    public boolean isActive() {
        return this.status == StatusSolicitacao.PENDENTE ||
                this.status == StatusSolicitacao.APROVADA;
    }
}
