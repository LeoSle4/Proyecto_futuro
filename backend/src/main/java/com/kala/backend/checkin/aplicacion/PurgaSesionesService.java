package com.kala.backend.checkin.aplicacion;

import com.kala.backend.checkin.aplicacion.puerto.SesionesCheckin;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Retención de datos: las sesiones del antiduplicado solo hacen falta mientras su ventana esté vigente. Pasado el
// periodo de retención se borran, para que no quede un historial de quién respondió y cuándo (R6). Los agregados no se
// purgan: son el producto.
@Service
public class PurgaSesionesService {

    // La cadencia más larga que admite el diseño es de 30 días: con menos retención se podría borrar una sesión
    // cuya ventana sigue abierta y dejar responder dos veces.
    static final int RETENCION_MINIMA_DIAS = 30;

    private final SesionesCheckin sesiones;
    private final int retencionDias;
    private final ZoneId zona;
    private final Clock reloj;

    public PurgaSesionesService(SesionesCheckin sesiones,
                                @Value("${kala.retencion.sesiones-dias:60}") int retencionDias,
                                @Value("${kala.ventana.zona}") String zona,
                                Clock reloj) {
        if (retencionDias < RETENCION_MINIMA_DIAS) {
            throw new IllegalArgumentException(
                    "kala.retencion.sesiones-dias debe ser al menos " + RETENCION_MINIMA_DIAS);
        }
        this.sesiones = sesiones;
        this.retencionDias = retencionDias;
        this.zona = ZoneId.of(zona);
        this.reloj = reloj;
    }

    // Devuelve cuántas sesiones borró.
    @Transactional
    public int purgar() {
        LocalDate limite = LocalDate.now(reloj.withZone(zona)).minusDays(retencionDias);
        return sesiones.borrarAnterioresA(limite);
    }
}
