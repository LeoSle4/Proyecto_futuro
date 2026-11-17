package com.kala.backend.identidad.infraestructura.persistencia;

import com.kala.backend.identidad.aplicacion.puerto.Usuarios;
import com.kala.backend.identidad.dominio.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, Usuarios {
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    @Override
    default Optional<Usuario> porId(Long id) {
        return findById(id);
    }

    @Override
    default boolean existeEmail(String email) {
        return existsByEmail(email);
    }

    @Override
    default Usuario guardar(Usuario usuario) {
        return save(usuario);
    }
}
