package com.kala.backend.instrumentos.infraestructura.persistencia;

import com.kala.backend.instrumentos.aplicacion.puerto.Presupuestos;
import com.kala.backend.instrumentos.dominio.PresupuestoPrivacidad;
import com.kala.backend.instrumentos.dominio.PresupuestoPrivacidadId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PresupuestoPrivacidadRepository extends JpaRepository<PresupuestoPrivacidad, PresupuestoPrivacidadId>, Presupuestos {

    // CHAR(7) en formato AAAA-MM se ordena igual que el calendario, así que alcanza con comparar el texto.
    @Override
    @Query("""
            SELECT p
            FROM PresupuestoPrivacidad p
            WHERE p.id.empresaId = :empresaId AND p.id.periodo <= :periodo
            ORDER BY p.id.periodo DESC
            LIMIT 1
            """)
    Optional<PresupuestoPrivacidad> vigenteEn(@Param("empresaId") Long empresaId, @Param("periodo") String periodo);

    @Override
    default PresupuestoPrivacidad guardar(PresupuestoPrivacidad presupuesto) {
        return save(presupuesto);
    }
}
