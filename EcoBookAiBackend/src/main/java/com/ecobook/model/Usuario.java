package com.ecobook.model;

import com.ecobook.model.enums.NecessidadeAcademica;
import com.ecobook.model.enums.Role;
import com.ecobook.validation.ProfileCompletionValidation;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.util.StringUtils;

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
@ToString(exclude = {"materiais", "solicitacoes"})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    @NotBlank(groups = ProfileCompletionValidation.class, message = "Nome is required")
    private String nome;

    @Column(length = 20)
    @NotNull(groups = ProfileCompletionValidation.class, message = "WhatsApp is required")
    @NotBlank(groups = ProfileCompletionValidation.class, message = "WhatsApp is required")
    @Pattern(
            regexp = "^\\+55\\d{11}$",
            groups = ProfileCompletionValidation.class,
            message = "WhatsApp must be in E.164 format (+55XXXXXXXXXXX)"
    )
    private String whatsapp;

    @Column(length = 100)
    @NotNull(groups = ProfileCompletionValidation.class, message = "Cidade is required")
    @NotBlank(groups = ProfileCompletionValidation.class, message = "Cidade is required")
    private String cidade;

    @Column(length = 100)
    @NotNull(groups = ProfileCompletionValidation.class, message = "Bairro is required")
    @NotBlank(groups = ProfileCompletionValidation.class, message = "Bairro is required")
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
    @Builder.Default
    private Set<NecessidadeAcademica> necessidadesAcademicas = new HashSet<>();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    @OneToMany(mappedBy = "doador", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Material> materiais = new ArrayList<>();

    @OneToMany(mappedBy = "estudante", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Solicitacao> solicitacoes = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    public boolean isPerfilCompleto() {
        return hasRequiredProfileFields();
    }

    public boolean hasRequiredProfileFields() {
        return StringUtils.hasText(this.email)
                && StringUtils.hasText(this.nome)
                && StringUtils.hasText(this.whatsapp)
                && StringUtils.hasText(this.cidade)
                && StringUtils.hasText(this.bairro);
    }

    public void refreshPerfilCompleto() {
        this.perfilCompleto = hasRequiredProfileFields();
    }
}
