package com.kala.backend.instrumentos.infraestructura.persistencia;

import com.kala.backend.instrumentos.aplicacion.puerto.OpcionesEscala;
import com.kala.backend.instrumentos.dominio.OpcionEscala;
import com.kala.backend.instrumentos.dominio.OpcionEscalaId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpcionEscalaRepository extends JpaRepository<OpcionEscala, OpcionEscalaId>, OpcionesEscala {

    @Query("""
            SELECT o
            FROM OpcionEscala o
            WHERE o.id.escalaId = :escalaId
            ORDER BY o.id.valor ASC
            """)
    List<OpcionEscala> listarPorEscala(@Param("escalaId") Long escalaId);

    @Override
    default List<OpcionEscala> deEscala(Long escalaId) {
        return listarPorEscala(escalaId);
    }
}
