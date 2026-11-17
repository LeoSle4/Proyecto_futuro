package com.kala.backend.checkin.infraestructura.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kala.backend.checkin.aplicacion.PurgaSesionesService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class PurgaProgramadaTest {

    @Test
    void laTareaDiariaLlamaAlCasoDeUso() {
        PurgaSesionesService servicio = mock(PurgaSesionesService.class);
        when(servicio.purgar()).thenReturn(3);

        new PurgaProgramada(servicio).ejecutar();

        verify(servicio).purgar();
    }

    @Test
    void estaProgramadaConUnCronConfigurableEnLaZonaDeLaAplicacion() throws Exception {
        Scheduled programada = PurgaProgramada.class.getMethod("ejecutar").getAnnotation(Scheduled.class);

        assertThat(programada).isNotNull();
        assertThat(programada.cron()).contains("kala.retencion.cron");
        assertThat(programada.zone()).contains("kala.ventana.zona");
    }

    @Test
    void siLaPurgaFallaLaTareaNoLanzaParaNoDetenerLaProgramacion() {
        PurgaSesionesService servicio = mock(PurgaSesionesService.class);
        when(servicio.purgar()).thenThrow(new IllegalStateException("base caída"));

        new PurgaProgramada(servicio).ejecutar();

        verify(servicio).purgar();
    }
}
