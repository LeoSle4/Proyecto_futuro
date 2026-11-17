package com.kala.backend.identidad.infraestructura.persistencia;

import com.kala.backend.identidad.dominio.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);
}
