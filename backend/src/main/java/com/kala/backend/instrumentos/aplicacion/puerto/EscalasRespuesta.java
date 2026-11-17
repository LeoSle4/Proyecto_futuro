package com.kala.backend.instrumentos.aplicacion.puerto;

import com.kala.backend.instrumentos.dominio.EscalaRespuesta;
import java.util.Optional;

// Puerto de salida: las escalas de respuesta.
public interface EscalasRespuesta {

    Optional<EscalaRespuesta> porId(Long id);
}
