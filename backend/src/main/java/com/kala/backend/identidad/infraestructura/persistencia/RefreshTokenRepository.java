package com.kala.backend.identidad.infraestructura.persistencia;

import com.kala.backend.identidad.aplicacion.puerto.RefreshTokens;
import com.kala.backend.identidad.dominio.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long>, RefreshTokens {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Una sola sentencia: solo gana quien encuentra el token libre (ni usado ni revocado).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE RefreshToken t SET t.usadoEn = :ahora
            WHERE t.id = :id AND t.usadoEn IS NULL AND t.revocadoEn IS NULL
            """)
    int marcarUsadoSiLibre(@Param("id") Long id, @Param("ahora") LocalDateTime ahora);

    @Override
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revocadoEn = :ahora WHERE t.familia = :familia AND t.revocadoEn IS NULL")
    int revocarFamilia(@Param("familia") String familia, @Param("ahora") LocalDateTime ahora);

    @Override
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revocadoEn = :ahora WHERE t.usuarioId = :usuarioId AND t.revocadoEn IS NULL")
    int revocarDeUsuario(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);

    @Override
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM RefreshToken t WHERE t.expiraEn < :limite")
    int borrarExpiradosAntesDe(@Param("limite") LocalDateTime limite);

    @Override
    default RefreshToken guardar(RefreshToken token) {
        return save(token);
    }

    @Override
    default Optional<RefreshToken> porHash(String hash) {
        return findByTokenHash(hash);
    }

    @Override
    default boolean marcarUsado(Long id, LocalDateTime ahora) {
        return marcarUsadoSiLibre(id, ahora) == 1;
    }
}
