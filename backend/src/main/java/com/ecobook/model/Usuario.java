package com.ecobook.model;

import com.ecobook.model.enums.NecessidadeAcademica;
import com.ecobook.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(
    name = "usuario",
    indexes = {
        @Index(name = "idx_usuario_email", columnList = "email", unique = true),
        @Index(name = "idx_usuario_google_id", columnList = "google_id"),
        @Index(name = "idx_usuario_perfil_completo", columnList = "perfil_completo")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"materials", "requests"})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private String id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(nullable = false, length = 20)
    private String whatsapp;

    @Column(nullable = false, length = 100)
    private String cidade;

    @Column(nullable = false, length = 100)
    private String bairro;

    @Column(length = 255)
    private String instituicao;

    @Column(nullable = false)
    @Builder.Default
    private Boolean perfilCompleto = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean consentimentoIa = false;

    @Column(unique = true, length = 255)
    private String googleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @ElementCollection(targetClass = NecessidadeAcademica.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_necessidades", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "necessidade")
    private Set<NecessidadeAcademica> necessidadesAcademicas = new HashSet<>();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    @OneToMany(mappedBy = "doador", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Material> materiais = new ArrayList<>();

    @OneToMany(mappedBy = "estudante", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Solicitacao> solicitacoes = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    public boolean isPerfilCompleto() {
        return this.perfilCompleto && this.email != null && this.nome != null &&
                this.whatsapp != null && this.cidade != null && this.bairro != null;
    }
}
