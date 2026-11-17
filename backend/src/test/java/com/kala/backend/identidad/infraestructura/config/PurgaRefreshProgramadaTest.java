package com.kala.backend.identidad.infraestructura.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kala.backend.identidad.aplicacion.RefrescoDeSesionService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class PurgaRefreshProgramadaTest {

    @Test
    void laTareaDiariaLlamaALaPurga() {
        RefrescoDeSesionService servicio = mock(RefrescoDeSesionService.class);
        when(servicio.purgarVencidos()).thenReturn(4);

        new PurgaRefreshProgramada(servicio).ejecutar();

        verify(servicio).purgarVencidos();
    }

    @Test
    void estaProgramadaConUnCronConfigurableEnLaZonaDeLaAplicacion() throws Exception {
        Scheduled s = PurgaRefreshProgramada.class.getMethod("ejecutar").getAnnotation(Scheduled.class);

        assertThat(s).isNotNull();
        assertThat(s.cron()).contains("kala.retencion.refresh-cron");
        assertThat(s.zone()).contains("kala.ventana.zona");
    }

    @Test
    void siLaPurgaFallaNoLanzaParaNoDetenerLaProgramacion() {
        RefrescoDeSesionService servicio = mock(RefrescoDeSesionService.class);
        when(servicio.purgarVencidos()).thenThrow(new IllegalStateException("base caída"));

        new PurgaRefreshProgramada(servicio).ejecutar();

        verify(servicio).purgarVencidos();
    }
}
