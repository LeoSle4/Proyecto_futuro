package com.kala.backend.checkin.infraestructura.persistencia;

import com.kala.backend.checkin.aplicacion.puerto.SesionesCheckin;
import com.kala.backend.checkin.dominio.SesionCheckin;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SesionCheckinRepository extends JpaRepository<SesionCheckin, String>, SesionesCheckin {

    // Atómico y sin carreras: devuelve 1 si insertó y 0 si el hash ya existía (envío duplicado).
    // No se usa save(): con un id asignado, save() haría un UPDATE silencioso en vez de fallar.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO sesion_checkin (session_hash, cuestionario_id, ventana_inicio)
            VALUES (:hash, :cuestionarioId, :ventanaInicio)
            ON CONFLICT (session_hash) DO NOTHING
            """, nativeQuery = true)
    int insertarSiNoExiste(@Param("hash") String hash,
                           @Param("cuestionarioId") Long cuestionarioId,
                           @Param("ventanaInicio") LocalDate ventanaInicio);

    @Override
    default boolean existe(String hash) {
        return existsById(hash);
    }

    @Override
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM sesion_checkin WHERE ventana_inicio < :fecha", nativeQuery = true)
    int borrarAnterioresA(@Param("fecha") LocalDate fecha);
}
