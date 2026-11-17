package com.kala.backend.checkin.infraestructura.config;

import com.kala.backend.checkin.aplicacion.PurgaSesionesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Tarea diaria que borra las sesiones vencidas del antiduplicado. Se apaga con kala.retencion.habilitada=false y su
// horario se cambia con kala.retencion.cron. Solo registra cuántas filas borró, nunca su contenido.
@Component
@ConditionalOnProperty(name = "kala.retencion.habilitada", havingValue = "true", matchIfMissing = true)
public class PurgaProgramada {

    private static final Logger log = LoggerFactory.getLogger(PurgaProgramada.class);

    private final PurgaSesionesService purga;

    public PurgaProgramada(PurgaSesionesService purga) {
        this.purga = purga;
    }

    @Scheduled(cron = "${kala.retencion.cron:0 30 3 * * *}", zone = "${kala.ventana.zona}")
    public void ejecutar() {
        try {
            int borradas = purga.purgar();
            log.info("Purga de sesiones del check-in: {} filas borradas", borradas);
        } catch (RuntimeException e) {
            // Una falla puntual no debe detener la programación: se reintenta al día siguiente.
            log.error("No se pudo purgar las sesiones del check-in ({})", e.getClass().getSimpleName());
        }
    }
}
