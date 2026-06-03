package com.ecobook.model;

import com.ecobook.model.enums.NecessidadeAcademica;
import com.ecobook.model.enums.Role;
import com.ecobook.validation.ProfileCompletionValidation;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
@ToString(exclude = {"passwordHash", "cpf", "fotoPerfilPath", "materiais", "solicitacoes"})
@SQLRestriction("deleted_at IS NULL")
public class Usuario extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    @NotBlank(groups = ProfileCompletionValidation.class, message = "Informe seu email")
    @Email(groups = ProfileCompletionValidation.class, message = "Informe um email valido")
    @Size(max = 255, groups = ProfileCompletionValidation.class, message = "O email deve ter no maximo 255 caracteres")
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
            message = "Use o formato E.164, por exemplo +5511999999999"
    )
    private String whatsapp;

    @Column(length = 11)
    @NotNull(groups = ProfileCompletionValidation.class, message = "Informe seu CPF")
    @NotBlank(groups = ProfileCompletionValidation.class, message = "Informe seu CPF")
    @Pattern(
            regexp = "^\\d{11}$",
            groups = ProfileCompletionValidation.class,
            message = "Informe um CPF com 11 dígitos"
    )
    private String cpf;

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

    @Column(name = "foto_perfil_path", length = 512)
    private String fotoPerfilPath;

    @Column(name = "foto_perfil_mime_type", length = 100)
    private String fotoPerfilMimeType;

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

    /**
     * Returns whether the user profile is currently marked as complete.
     * @return true when the condition holds; otherwise false
     */
    public boolean isPerfilCompleto() {
        return hasRequiredProfileFields();
    }

    /**
     * Checks whether the user has all required fields filled for a complete profile.
     * @return true when the condition holds; otherwise false
     */
    public boolean hasRequiredProfileFields() {
        return StringUtils.hasText(this.email)
                && StringUtils.hasText(this.nome)
                && StringUtils.hasText(this.whatsapp)
                && StringUtils.hasText(this.cpf)
                && StringUtils.hasText(this.cidade)
                && StringUtils.hasText(this.bairro);
    }

    /**
     * Recomputes the profile completeness flag from the current field values.
     */
    public void refreshPerfilCompleto() {
        this.perfilCompleto = hasRequiredProfileFields();
    }
}
