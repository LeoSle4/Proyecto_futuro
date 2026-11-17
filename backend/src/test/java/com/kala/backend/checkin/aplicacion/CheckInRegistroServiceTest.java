package com.kala.backend.checkin.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kala.backend.checkin.aplicacion.puerto.AgregadosCheckin;
import com.kala.backend.checkin.aplicacion.puerto.ItemsCuestionario.ItemCuestionarioInfo;
import com.kala.backend.checkin.aplicacion.puerto.ItemsCuestionario;
import com.kala.backend.checkin.aplicacion.puerto.SesionesCheckin;
import com.kala.backend.checkin.dominio.CheckInDuplicadoException;
import com.kala.backend.checkin.dominio.CheckInInvalidoException;
import com.kala.backend.checkin.dominio.ConfiguracionLdpNoVigenteException;
import com.kala.backend.checkin.dominio.ItemCuestionarioNoEncontradoException;
import com.kala.backend.privacidad.dominio.SesionHasher;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CheckInRegistroServiceTest {

    private static final String SECRETO = "secreto-de-prueba";
    private static final String ZONA = "America/Lima";
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
    private static final LocalDate MIERCOLES = LocalDate.of(2026, 10, 14);

    // 2026-10-14T15:00Z = miércoles 10:00 en Lima; su ventana empieza el lunes 2026-10-12.
    private static final Clock MIERCOLES_EN_LIMA = Clock.fixed(Instant.parse("2026-10-14T15:00:00Z"), ZoneOffset.UTC);

    private ItemsCuestionario items;
    private SesionesCheckin sesiones;
    private AgregadosCheckin agregados;
    private CheckInRegistroService service;

    @BeforeEach
    void setUp() {
        items = mock(ItemsCuestionario.class);
        sesiones = mock(SesionesCheckin.class);
        agregados = mock(AgregadosCheckin.class);
        service = new CheckInRegistroService(items, sesiones, agregados, SECRETO, ZONA, MIERCOLES_EN_LIMA);
    }

    private void itemVigente() {
        when(items.buscar(9L)).thenReturn(Optional.of(ItemCuestionarioInfo.pulso(3L, 2L)));
    }

    @Test
    void respuestaVerdadera_guardaLaSesionYSumaUnoAlAgregadoDelDia() {
        itemVigente();
        when(sesiones.insertarSiNoExiste(anyString(), eq(3L), eq(LUNES))).thenReturn(1);

        service.registrar(7L, 5L, new CheckInEnvioRequest(9L, 2L, true));

        verify(sesiones).insertarSiNoExiste(SesionHasher.hashDeSesion(SECRETO, 7L, 3L, LUNES), 3L, LUNES);
        verify(agregados).registrarRespuesta(5L, 9L, 2L, MIERCOLES, BigDecimal.ONE);
    }

    @Test
    void respuestaFalsa_cuentaEnElTotalPeroSumaCero() {
        itemVigente();
        when(sesiones.insertarSiNoExiste(anyString(), eq(3L), eq(LUNES))).thenReturn(1);

        service.registrar(7L, 5L, new CheckInEnvioRequest(9L, 2L, false));

        verify(agregados).registrarRespuesta(5L, 9L, 2L, MIERCOLES, BigDecimal.ZERO);
    }

    @Test
    void siLaSesionYaExiste_lanza409YNoTocaElAgregado() {
        itemVigente();
        when(sesiones.insertarSiNoExiste(anyString(), eq(3L), eq(LUNES))).thenReturn(0);

        assertThatThrownBy(() -> service.registrar(7L, 5L, new CheckInEnvioRequest(9L, 2L, true)))
                .isInstanceOf(CheckInDuplicadoException.class);

        verifyNoInteractions(agregados);
    }

    @Test
    void siLaConfiguracionNoEsLaVigente_lanzaYNoGuardaNada() {
        itemVigente();

        assertThatThrownBy(() -> service.registrar(7L, 5L, new CheckInEnvioRequest(9L, 99L, true)))
                .isInstanceOf(ConfiguracionLdpNoVigenteException.class);

        verifyNoInteractions(sesiones, agregados);
    }

    @Test
    void siElItemNoExiste_lanzaYNoGuardaNada() {
        when(items.buscar(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(7L, 5L, new CheckInEnvioRequest(9L, 2L, true)))
                .isInstanceOf(ItemCuestionarioNoEncontradoException.class);

        verifyNoInteractions(sesiones, agregados);
    }

    @Test
    void lasRespuestasDeUnaMismaSemanaComparten_hash_yLaSemanaSiguienteUsaOtro() {
        itemVigente();
        when(sesiones.insertarSiNoExiste(anyString(), eq(3L), org.mockito.ArgumentMatchers.any())).thenReturn(1);
        ArgumentCaptor<String> hashes = ArgumentCaptor.forClass(String.class);

        // miércoles, domingo 23:00 en Lima (mismo semana) y lunes 00:30 en Lima (semana nueva)
        for (String instante : new String[] {"2026-10-14T15:00:00Z", "2026-10-19T04:00:00Z", "2026-10-19T05:30:00Z"}) {
            Clock reloj = Clock.fixed(Instant.parse(instante), ZoneOffset.UTC);
            new CheckInRegistroService(items, sesiones, agregados, SECRETO, ZONA, reloj)
                    .registrar(7L, 5L, new CheckInEnvioRequest(9L, 2L, true));
        }

        verify(sesiones, org.mockito.Mockito.times(3)).insertarSiNoExiste(hashes.capture(), eq(3L), org.mockito.ArgumentMatchers.any());
        assertThat(hashes.getAllValues().get(0)).isEqualTo(hashes.getAllValues().get(1));
        assertThat(hashes.getAllValues().get(2)).isNotEqualTo(hashes.getAllValues().get(0));
    }

    // ---- Laplace y baterias ----

    private static ItemCuestionarioInfo laplace(long cuestionario, long config, int cadencia, Integer k, int total) {
        return new ItemCuestionarioInfo(cuestionario, config, "LAPLACE", cadencia, k, total);
    }

    @Test
    void itemLaplace_sumaElValorRuidosoSinRecortarlo() {
        when(items.buscar(21L)).thenReturn(Optional.of(laplace(4L, 8L, 14, 1, 5)));
        when(sesiones.insertarSiNoExiste(anyString(), eq(4L), org.mockito.ArgumentMatchers.any())).thenReturn(1);

        service.registrar(7L, 5L, new CheckInEnvioRequest(21L, 8L, null, new BigDecimal("-1.3700")));

        verify(agregados).registrarRespuesta(5L, 21L, 8L, MIERCOLES, new BigDecimal("-1.3700"));
    }

    @Test
    void itemLaplaceSinValorRuidoso_esInvalidoYNoGuardaNada() {
        when(items.buscar(21L)).thenReturn(Optional.of(laplace(4L, 8L, 14, 1, 5)));

        assertThatThrownBy(() -> service.registrar(7L, 5L, new CheckInEnvioRequest(21L, 8L, true)))
                .isInstanceOf(CheckInInvalidoException.class);

        verifyNoInteractions(sesiones, agregados);
    }

    @Test
    void itemRandomizedResponseSinRespuestaRuidosa_esInvalidoYNoGuardaNada() {
        itemVigente();

        assertThatThrownBy(() -> service.registrar(7L, 5L, new CheckInEnvioRequest(9L, 2L, null, new BigDecimal("1"))))
                .isInstanceOf(CheckInInvalidoException.class);

        verifyNoInteractions(sesiones, agregados);
    }

    @Test
    void loteDeUnaBateria_usaUnaSolaSesionYSumaCadaItem() {
        when(items.buscar(31L)).thenReturn(Optional.of(laplace(5L, 8L, 14, 2, 8)));
        when(items.buscar(32L)).thenReturn(Optional.of(laplace(5L, 8L, 14, 2, 8)));
        when(sesiones.insertarSiNoExiste(anyString(), eq(5L), org.mockito.ArgumentMatchers.any())).thenReturn(1);

        service.registrarLote(7L, 5L, java.util.List.of(
                new CheckInEnvioRequest(31L, 8L, null, new BigDecimal("1.5")),
                new CheckInEnvioRequest(32L, 8L, null, new BigDecimal("0.2"))));

        verify(sesiones, org.mockito.Mockito.times(1)).insertarSiNoExiste(anyString(), eq(5L), org.mockito.ArgumentMatchers.any());
        verify(agregados).registrarRespuesta(5L, 31L, 8L, MIERCOLES, new BigDecimal("1.5"));
        verify(agregados).registrarRespuesta(5L, 32L, 8L, MIERCOLES, new BigDecimal("0.2"));
    }

    @Test
    void loteConMasItemsQueK_esInvalido() {
        when(items.buscar(anyLong())).thenReturn(Optional.of(laplace(5L, 8L, 14, 2, 8)));

        assertThatThrownBy(() -> service.registrarLote(7L, 5L, java.util.List.of(
                new CheckInEnvioRequest(31L, 8L, null, BigDecimal.ONE),
                new CheckInEnvioRequest(32L, 8L, null, BigDecimal.ONE),
                new CheckInEnvioRequest(33L, 8L, null, BigDecimal.ONE))))
                .isInstanceOf(CheckInInvalidoException.class);

        verifyNoInteractions(sesiones, agregados);
    }

    @Test
    void loteConItemsDeDosCuestionarios_oRepetidos_esInvalido() {
        when(items.buscar(31L)).thenReturn(Optional.of(laplace(5L, 8L, 14, 2, 8)));
        when(items.buscar(41L)).thenReturn(Optional.of(laplace(6L, 9L, 14, 2, 7)));

        assertThatThrownBy(() -> service.registrarLote(7L, 5L, java.util.List.of(
                new CheckInEnvioRequest(31L, 8L, null, BigDecimal.ONE),
                new CheckInEnvioRequest(41L, 9L, null, BigDecimal.ONE))))
                .isInstanceOf(CheckInInvalidoException.class);
        assertThatThrownBy(() -> service.registrarLote(7L, 5L, java.util.List.of(
                new CheckInEnvioRequest(31L, 8L, null, BigDecimal.ONE),
                new CheckInEnvioRequest(31L, 8L, null, BigDecimal.ONE))))
                .isInstanceOf(CheckInInvalidoException.class);

        verifyNoInteractions(sesiones, agregados);
    }

    @Test
    void loteConUnaConfiguracionNoVigente_lanza422YNoGuardaNada() {
        when(items.buscar(31L)).thenReturn(Optional.of(laplace(5L, 8L, 14, 2, 8)));
        when(items.buscar(32L)).thenReturn(Optional.of(laplace(5L, 8L, 14, 2, 8)));

        assertThatThrownBy(() -> service.registrarLote(7L, 5L, java.util.List.of(
                new CheckInEnvioRequest(31L, 8L, null, BigDecimal.ONE),
                new CheckInEnvioRequest(32L, 99L, null, BigDecimal.ONE))))
                .isInstanceOf(ConfiguracionLdpNoVigenteException.class);

        verifyNoInteractions(sesiones, agregados);
    }

    @Test
    void cuestionarioQuincenal_usaUnaVentanaDeCatorceDias() {
        when(items.buscar(21L)).thenReturn(Optional.of(laplace(4L, 8L, 14, 1, 5)));
        when(sesiones.insertarSiNoExiste(anyString(), eq(4L), org.mockito.ArgumentMatchers.any())).thenReturn(1);
        ArgumentCaptor<LocalDate> ventanas = ArgumentCaptor.forClass(LocalDate.class);

        // Los bloques quincenales arrancan el 2026-10-05: ese lunes y el siguiente comparten ventana; el tercero abre otra.
        for (String instante : new String[] {"2026-10-05T15:00:00Z", "2026-10-12T15:00:00Z", "2026-10-19T15:00:00Z"}) {
            Clock reloj = Clock.fixed(Instant.parse(instante), ZoneOffset.UTC);
            new CheckInRegistroService(items, sesiones, agregados, SECRETO, ZONA, reloj)
                    .registrar(7L, 5L, new CheckInEnvioRequest(21L, 8L, null, BigDecimal.ONE));
        }

        verify(sesiones, org.mockito.Mockito.times(3)).insertarSiNoExiste(anyString(), eq(4L), ventanas.capture());
        assertThat(ventanas.getAllValues().get(0)).isEqualTo(ventanas.getAllValues().get(1));
        assertThat(ventanas.getAllValues().get(2)).isEqualTo(ventanas.getAllValues().get(0).plusDays(14));
    }
}
