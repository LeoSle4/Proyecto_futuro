package com.kala.backend.instrumentos.aplicacion.puerto;

import com.kala.backend.instrumentos.dominio.Cuestionario;
import java.util.List;
import java.util.Optional;

// Puerto de salida: los cuestionarios y cuáles tiene activos cada empresa.
public interface Cuestionarios {

    Optional<Cuestionario> porId(Long id);

    List<Cuestionario> activosDeEmpresa(Long empresaId);

    // El catálogo completo, ordenado por código, para que se pueda elegir cuál activar.
    List<Cuestionario> todos();
}
