
package com.kala.backend.organizacion.infraestructura.persistencia;

import com.kala.backend.organizacion.aplicacion.puerto.Equipos;
import com.kala.backend.organizacion.dominio.Equipo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipoRepository extends JpaRepository<Equipo, Long>, Equipos {

    List<Equipo> findByEmpresaId(Long empresaId);

    Optional<Equipo> findByIdAndEmpresaId(Long id, Long empresaId);

    boolean existsByEmpresaId(Long empresaId);

    @Query("""
            SELECT e
            FROM Equipo e
            WHERE e.empresaId = :empresaId
            ORDER BY e.nombre ASC
            """)
    List<Equipo> listarPorEmpresaOrdenado(
            @Param("empresaId") Long empresaId);

    @Query("""
            SELECT COUNT(e)
            FROM Equipo e
            WHERE e.empresaId = :empresaId
            """)
    long contarPorEmpresa(@Param("empresaId") Long empresaId);

    @Override
    default List<Equipo> todos() {
        return findAll();
    }

    @Override
    default List<Equipo> deEmpresa(Long empresaId) {
        return findByEmpresaId(empresaId);
    }

    @Override
    default Optional<Equipo> porId(Long id) {
        return findById(id);
    }

    @Override
    default Optional<Equipo> porIdEnEmpresa(Long id, Long empresaId) {
        return findByIdAndEmpresaId(id, empresaId);
    }

    @Override
    default boolean tieneEquipos(Long empresaId) {
        return existsByEmpresaId(empresaId);
    }

    @Override
    default Equipo guardar(Equipo equipo) {
        return save(equipo);
    }

    @Override
    default void eliminar(Equipo equipo) {
        delete(equipo);
    }
}
