package com.kala.backend.identidad.aplicacion.puerto;

import com.kala.backend.identidad.dominio.Usuario;
import java.util.Optional;

// Puerto de salida: las cuentas de usuario.
public interface Usuarios {

    Optional<Usuario> porId(Long id);

    // El correo se compara ya normalizado (sin espacios y en minúsculas).
    boolean existeEmail(String email);

    Usuario guardar(Usuario usuario);
}
