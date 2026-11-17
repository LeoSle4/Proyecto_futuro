package com.kala.backend.checkin.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kala.backend.checkin.aplicacion.puerto.SesionesCheckin;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class PurgaSesionesServiceTest {

    // 2026-11-18 01:00 UTC = 2026-11-17 20:00 en Lima: "hoy" en Lima es el 17.
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-18T01:00:00Z"), ZoneId.of("UTC"));
    private final SesionesCheckin sesiones = mock(SesionesCheckin.class);

    @Test
    void borraLasSesionesMasViejasQueLaRetencionContandoElDiaLocal() {
        when(sesiones.borrarAnterioresA(LocalDate.of(2026, 9, 18))).thenReturn(42);

        int borradas = new PurgaSesionesService(sesiones, 60, "America/Lima", reloj).purgar();

        assertThat(borradas).isEqualTo(42);
        verify(sesiones).borrarAnterioresA(LocalDate.of(2026, 9, 18));
    }

    @Test
    void unaRetencionMenorQueLaCadenciaMasLargaSeRechazaAlArrancar() {
        for (int dias : new int[] {-1, 0, 7, 29}) {
            assertThatThrownBy(() -> new PurgaSesionesService(sesiones, dias, "America/Lima", reloj))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("30");
        }
        verifyNoInteractions(sesiones);
    }

    @Test
    void laRetencionMinimaDe30DiasSeAcepta() {
        when(sesiones.borrarAnterioresA(LocalDate.of(2026, 10, 18))).thenReturn(0);

        assertThat(new PurgaSesionesService(sesiones, 30, "America/Lima", reloj).purgar()).isZero();
    }
}
