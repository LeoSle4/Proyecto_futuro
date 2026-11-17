package com.kala.backend.instrumentos.infraestructura.persistencia;

import com.kala.backend.instrumentos.aplicacion.puerto.ItemsDeCuestionario;
import com.kala.backend.instrumentos.dominio.ItemCuestionario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemCuestionarioRepository extends JpaRepository<ItemCuestionario, Long>, ItemsDeCuestionario {

    @Query("""
            SELECT i
            FROM ItemCuestionario i
            WHERE i.cuestionarioId = :cuestionarioId
            ORDER BY i.orden ASC
            """)
    List<ItemCuestionario> listarPorCuestionario(@Param("cuestionarioId") Long cuestionarioId);

    @Override
    default List<ItemCuestionario> deCuestionario(Long cuestionarioId) {
        return listarPorCuestionario(cuestionarioId);
    }
}
