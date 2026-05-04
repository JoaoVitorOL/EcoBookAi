package com.ecobook.model;

import com.ecobook.model.enums.*;
import jakarta.persistence.*;
import lombok.*;

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
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "doador_id", nullable = false)
    private Usuario doador;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Disciplina disciplina;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelEnsino nivelEnsino;

    @Column
    private Integer ano;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SistemaEnsino sistemaEnsino;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoConservacao estadoConservacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusMaterial status = StatusMaterial.DISPONIVEL;

    @Column(length = 500)
    private String imagemUrl;

    @Column(length = 100)
    private String uploadId;

    @Column(nullable = false, length = 100)
    private String cidade;

    @Column(nullable = false, length = 100)
    private String bairro;

    @Column
    private Integer dataPublicacao;

    @Enumerated(EnumType.STRING)
    @Column
    private StatusIA statusIa;

    @Column
    private Double confiancaIa;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    @OneToMany(mappedBy = "material", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
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
