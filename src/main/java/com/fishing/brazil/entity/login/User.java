package com.fishing.brazil.entity.login;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "verification_code", length = 6)
    private String verificationCode;

    @Column(name = "verification_code_expires_at")
    private java.time.LocalDateTime verificationCodeExpiresAt;

    // Tentativas erradas contra o codigo atual. Zerado a cada codigo novo e a cada
    // uso bem-sucedido; ao estourar o teto o codigo e destruido em vez de continuar
    // disponivel para o proximo palpite.
    //
    // Integer, e a coluna e anulavel: com ddl-auto=update, adicionar uma coluna
    // NOT NULL a uma tabela que ja tem linhas falha no PostgreSQL, e a aplicacao
    // subiria sem a coluna que passou a ler.
    @Column(name = "verification_attempts")
    private Integer verificationAttempts;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "tb_user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
