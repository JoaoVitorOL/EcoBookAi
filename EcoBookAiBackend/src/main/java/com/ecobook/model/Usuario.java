package com.ecobook.model;

import com.ecobook.model.enums.NecessidadeAcademica;
import com.ecobook.model.enums.Role;
import com.ecobook.validation.ProfileCompletionValidation;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(
    name = "usuario",
    indexes = {
        @Index(name = "idx_usuario_email", columnList = "email", unique = true),
        @Index(name = "idx_usuario_perfil_completo", columnList = "perfil_completo")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"passwordHash", "materiais", "solicitacoes"})
@SQLRestriction("deleted_at IS NULL")
public class Usuario extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 255)
    @NotBlank(groups = ProfileCompletionValidation.class, message = "Informe seu nome")
    private String nome;

    @Column(length = 20)
    @NotNull(groups = ProfileCompletionValidation.class, message = "Informe um WhatsApp")
    @NotBlank(groups = ProfileCompletionValidation.class, message = "Informe um WhatsApp")
    @Pattern(
            regexp = "^\\+55\\d{11}$",
            groups = ProfileCompletionValidation.class,
            message = "Use o formato E.164 (+55XXXXXXXXXXX)"
    )
    private String whatsapp;

    @Column(length = 100)
    @NotNull(groups = ProfileCompletionValidation.class, message = "Informe sua cidade")
    @NotBlank(groups = ProfileCompletionValidation.class, message = "Informe sua cidade")
    private String cidade;

    @Column(length = 100)
    @NotNull(groups = ProfileCompletionValidation.class, message = "Informe seu bairro")
    @NotBlank(groups = ProfileCompletionValidation.class, message = "Informe seu bairro")
    private String bairro;

    @Column(length = 255)
    private String instituicao;

    @Column(name = "fcm_token", length = 512)
    private String fcmToken;

    @Column(nullable = false)
    @Builder.Default
    private Boolean perfilCompleto = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean consentimentoIa = false;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "role_enum")
    @Builder.Default
    private Role role = Role.USER;

    @ElementCollection(targetClass = NecessidadeAcademica.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_necessidades", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "necessidade", columnDefinition = "necessidade_academica_enum")
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
