package com.kala.backend.instrumentos.infraestructura.persistencia;

import com.kala.backend.instrumentos.aplicacion.puerto.EscalasRespuesta;
import com.kala.backend.instrumentos.dominio.EscalaRespuesta;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EscalaRespuestaRepository extends JpaRepository<EscalaRespuesta, Long>, EscalasRespuesta {

    @Override
    default Optional<EscalaRespuesta> porId(Long id) {
        return findById(id);
    }
}
