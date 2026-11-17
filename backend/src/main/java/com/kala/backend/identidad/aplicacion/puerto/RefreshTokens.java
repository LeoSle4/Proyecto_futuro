package com.kala.backend.identidad.aplicacion.puerto;

import com.kala.backend.identidad.dominio.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;

// Puerto de salida: las sesiones de larga duración. Las operaciones que cambian estado son atómicas para que dos
// renovaciones simultáneas del mismo token no puedan ganar las dos.
public interface RefreshTokens {

    RefreshToken guardar(RefreshToken token);

    Optional<RefreshToken> porHash(String hash);

    // true solo si este llamado fue el que lo marcó: el token no estaba usado ni revocado.
    boolean marcarUsado(Long id, LocalDateTime ahora);

    int revocarFamilia(String familia, LocalDateTime ahora);

    int revocarDeUsuario(Long usuarioId, LocalDateTime ahora);

    int borrarExpiradosAntesDe(LocalDateTime limite);
}
