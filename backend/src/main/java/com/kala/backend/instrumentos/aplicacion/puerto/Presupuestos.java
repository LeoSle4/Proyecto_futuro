package com.kala.backend.instrumentos.aplicacion.puerto;

import com.kala.backend.instrumentos.dominio.PresupuestoPrivacidad;
import java.util.Optional;

// Puerto de salida: el ε máximo por persona que fijó una empresa para un mes (AAAA-MM).
public interface Presupuestos {

    Optional<PresupuestoPrivacidad> deEmpresaYPeriodo(Long empresaId, String periodo);
}
