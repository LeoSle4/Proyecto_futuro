package com.kala.backend.checkin.aplicacion.puerto;

import java.math.BigDecimal;
import java.time.LocalDate;

// Puerto de salida: los contadores ruidosos por equipo, ítem, configuración y día. Nunca guarda quién respondió (R1).
public interface AgregadosCheckin {

    // Upsert atómico: crea el contador del día o le suma la respuesta, sin leer primero.
    int registrarRespuesta(Long equipoId, Long itemId, Long configuracionLdpId, LocalDate fecha, BigDecimal suma);
}
