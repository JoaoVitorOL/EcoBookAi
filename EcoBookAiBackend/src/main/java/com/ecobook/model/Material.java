package com.ecobook.model;

import com.ecobook.model.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "material",
    indexes = {
        @Index(name = "idx_material_status", columnList = "status"),
        @Index(name = "idx_material_status_disciplina", columnList = "status, disciplina"),
        @Index(name = "idx_material_status_nivel_ensino", columnList = "status, nivel_ensino"),
        @Index(name = "idx_material_sistema_ensino", columnList = "sistema_ensino"),
        @Index(name = "idx_material_cidade_bairro", columnList = "cidade, bairro"),
        @Index(name = "idx_material_data_publicacao", columnList = "data_publicacao"),
        @Index(name = "idx_material_doador_id", columnList = "doador_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"doador", "solicitacoes"})
@SQLRestriction("deleted_at IS NULL")
public class Material extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doador_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Usuario doador;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(length = 255)
    private String autor;

    @Column(length = 255)
    private String editora;

    @Column(length = 2000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "disciplina_enum")
    private Disciplina disciplina;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "nivel_ensino_enum")
    private NivelEnsino nivelEnsino;

    @Column
    private Integer ano;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "sistema_ensino_enum")
    private SistemaEnsino sistemaEnsino;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "estado_conservacao_enum")
    private EstadoConservacao estadoConservacao;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "status_material_enum")
    @Builder.Default
    private StatusMaterial status = StatusMaterial.DISPONIVEL;

    @Column(length = 500)
    private String imagemUrl;

    @Column(length = 500)
    private String imagemVersoUrl;

    @Column(length = 100)
    private String uploadId;

    @Column(name = "upload_tracking_id")
    private UUID uploadTrackingId;

    @Column(nullable = false, length = 100)
    private String cidade;

    @Column(nullable = false, length = 100)
    private String bairro;

    @Column
    private Integer dataPublicacao;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "status_ia_enum")
    private StatusIA statusIa;

    @Column(precision = 3, scale = 2)
    private BigDecimal confiancaIa;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    @Column(name = "doado_em")
    private LocalDateTime doadoEm;

    @OneToMany(mappedBy = "material", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Solicitacao> solicitacoes = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    /**
     * Check if material has an active approved request
     */
    public boolean hasApprovedRequest() {
        return this.solicitacoes.stream()
                .anyMatch(s -> s.getStatus() == StatusSolicitacao.APROVADA);
    }

    /**
     * Get the approved request for this material
     */
    public Solicitacao getApprovedRequest() {
        return this.solicitacoes.stream()
                .filter(s -> s.getStatus() == StatusSolicitacao.APROVADA)
                .findFirst()
                .orElse(null);
    }
}
