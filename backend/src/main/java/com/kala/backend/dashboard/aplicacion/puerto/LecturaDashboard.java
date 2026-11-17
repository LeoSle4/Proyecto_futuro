package com.kala.backend.dashboard.aplicacion.puerto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Puerto de salida: lectura de los agregados de varios módulos para estimar un equipo en una ventana.
public interface LecturaDashboard {

    // Un ítem agregado en la ventana. En Randomized Response, suma = respuestas reportadas como "sí" y n = total.
    record FilaItem(
            Long itemId,
            String tema,
            Long configuracionLdpId,
            String mecanismo,
            BigDecimal probabilidadVerdad,
            BigDecimal epsilon,
            int valorMin,
            int valorMax,
            BigDecimal suma,
            long n
    ) {
    }

    boolean equipoPerteneceAEmpresa(Long equipoId, Long empresaId);

    List<FilaItem> agregadosDeVentana(Long equipoId, LocalDate desde, LocalDate hasta);
}
