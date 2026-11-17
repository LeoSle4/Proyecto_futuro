package com.kala.backend.instrumentos.aplicacion.puerto;

import com.kala.backend.instrumentos.dominio.PresupuestoPrivacidad;
import java.util.Optional;

// Puerto de salida: el ε máximo por persona que fijó una empresa. Un presupuesto fijado en un mes sigue vigente
// los meses siguientes hasta que se fije otro.
public interface Presupuestos {

    // El último presupuesto fijado en o antes del periodo (AAAA-MM). Vacío si la empresa nunca fijó uno.
    Optional<PresupuestoPrivacidad> vigenteEn(Long empresaId, String periodo);

    // Crea o reemplaza el presupuesto de ese mes.
    PresupuestoPrivacidad guardar(PresupuestoPrivacidad presupuesto);
}
