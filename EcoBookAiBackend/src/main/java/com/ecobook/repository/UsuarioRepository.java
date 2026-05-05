package com.ecobook.repository;

import com.ecobook.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByGoogleId(String googleId);

    @Query("""
            select u
            from Usuario u
            where u.googleId = ?1
               or lower(u.email) = lower(?2)
            """)
    Optional<Usuario> findByGoogleIdOrEmailIgnoreCase(String googleId, String email);

    @Query("""
            select u
            from Usuario u
            where lower(u.email) = lower(?1)
            """)
    Optional<Usuario> findByEmailIgnoreCase(String email);

    default Optional<Usuario> findByGoogleIdOrCreateNew(String googleId, String email) {
        return findByGoogleIdOrEmailIgnoreCase(googleId, email);
    }
}
