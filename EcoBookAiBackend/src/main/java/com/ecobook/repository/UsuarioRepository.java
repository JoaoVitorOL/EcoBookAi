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
            where lower(u.email) = lower(?1)
            """)
    Optional<Usuario> findByEmailIgnoreCase(String email);
}
