package com.kala.backend.instrumentos.infraestructura.persistencia;

import com.kala.backend.instrumentos.aplicacion.puerto.ConfiguracionesLdp;
import com.kala.backend.instrumentos.dominio.ConfiguracionLdp;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConfiguracionLdpRepository extends JpaRepository<ConfiguracionLdp, Long>, ConfiguracionesLdp {

    // La configuración vigente de un ítem es la que el ítem referencia hoy.
    @Query("""
            SELECT c
            FROM ConfiguracionLdp c
            WHERE c.id = (SELECT i.configuracionLdpId FROM ItemCuestionario i WHERE i.id = :itemId)
            """)
    Optional<ConfiguracionLdp> buscarVigenteDeItem(@Param("itemId") Long itemId);

    @Override
    default Optional<ConfiguracionLdp> porId(Long id) {
        return findById(id);
    }
}
