package com.kala.backend.ayuda.infraestructura.persistencia;

import com.kala.backend.ayuda.aplicacion.puerto.SolicitudesAyuda;
import com.kala.backend.ayuda.dominio.SolicitudAyuda;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudAyudaRepository extends JpaRepository<SolicitudAyuda, Long>, SolicitudesAyuda {

    List<SolicitudAyuda> findByUsuarioIdOrderByCreadoEnDesc(Long usuarioId);

    // La empresa de una solicitud se deriva de la persona: empleado -> equipo -> empresa (3FN, no se guarda).
    @Query("""
            SELECT s
            FROM SolicitudAyuda s
            WHERE s.usuarioId IN (
                SELECT e.usuarioId FROM EmpleadoEquipo e
                WHERE e.equipoId IN (SELECT q.id FROM Equipo q WHERE q.empresaId = :empresaId))
            ORDER BY s.creadoEn DESC
            """)
    List<SolicitudAyuda> listarDeEmpresa(@Param("empresaId") Long empresaId);

    @Query("""
            SELECT s
            FROM SolicitudAyuda s
            WHERE s.id = :id
              AND s.usuarioId IN (
                SELECT e.usuarioId FROM EmpleadoEquipo e
                WHERE e.equipoId IN (SELECT q.id FROM Equipo q WHERE q.empresaId = :empresaId))
            """)
    Optional<SolicitudAyuda> buscarDeEmpresa(@Param("id") Long id, @Param("empresaId") Long empresaId);

    @Override
    default SolicitudAyuda guardar(SolicitudAyuda solicitud) {
        return save(solicitud);
    }

    @Override
    default List<SolicitudAyuda> deUsuario(Long usuarioId) {
        return findByUsuarioIdOrderByCreadoEnDesc(usuarioId);
    }

    @Override
    default List<SolicitudAyuda> deEmpresa(Long empresaId) {
        return listarDeEmpresa(empresaId);
    }
}
