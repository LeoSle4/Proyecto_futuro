package com.kala.backend.instrumentos.aplicacion.puerto;

import com.kala.backend.instrumentos.dominio.OpcionEscala;
import java.util.List;

// Puerto de salida: las etiquetas de cada valor de una escala, en orden.
public interface OpcionesEscala {

    List<OpcionEscala> deEscala(Long escalaId);
}
