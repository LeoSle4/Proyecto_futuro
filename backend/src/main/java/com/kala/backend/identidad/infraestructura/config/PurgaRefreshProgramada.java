package com.kala.backend.identidad.infraestructura.config;

import com.kala.backend.identidad.aplicacion.RefrescoDeSesionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Tarea diaria que borra los refresh tokens vencidos hace más de una semana. Se apaga con kala.retencion.habilitada=false
// y su horario se cambia con kala.retencion.refresh-cron. Solo registra cuántas filas borró.
@Component
@ConditionalOnProperty(name = "kala.retencion.habilitada", havingValue = "true", matchIfMissing = true)
public class PurgaRefreshProgramada {

    private static final Logger log = LoggerFactory.getLogger(PurgaRefreshProgramada.class);

    private final RefrescoDeSesionService refresco;

    public PurgaRefreshProgramada(RefrescoDeSesionService refresco) {
        this.refresco = refresco;
    }

    @Scheduled(cron = "${kala.retencion.refresh-cron:0 45 3 * * *}", zone = "${kala.ventana.zona}")
    public void ejecutar() {
        try {
            log.info("Purga de sesiones de larga duración: {} filas borradas", refresco.purgarVencidos());
        } catch (RuntimeException e) {
            log.error("No se pudo purgar las sesiones de larga duración ({})", e.getClass().getSimpleName());
        }
    }
}
