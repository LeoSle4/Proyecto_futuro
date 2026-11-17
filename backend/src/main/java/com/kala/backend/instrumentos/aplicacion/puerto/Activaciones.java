package com.kala.backend.instrumentos.aplicacion.puerto;

import com.kala.backend.instrumentos.dominio.EmpresaCuestionario;
import java.util.Optional;

// Puerto de salida: qué cuestionarios activó cada empresa.
public interface Activaciones {

    Optional<EmpresaCuestionario> porId(Long empresaId, Long cuestionarioId);

    EmpresaCuestionario guardar(EmpresaCuestionario activacion);
}
