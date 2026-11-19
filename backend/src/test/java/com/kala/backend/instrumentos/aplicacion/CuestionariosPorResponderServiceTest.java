package com.kala.backend.instrumentos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kala.backend.checkin.aplicacion.puerto.SesionesCheckin;
import com.kala.backend.privacidad.dominio.SesionHasher;
import com.kala.backend.privacidad.dominio.VentanaSemanal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CuestionariosPorResponderServiceTest {

    private static final String SECRETO = "secreto-de-prueba";
    private static final String ZONA = "America/Lima";
    private static final Long EMPRESA = 1L;
    private static final Long PERSONA = 7L;

    // 2026-10-14T15:00Z = miércoles 10:00 en Lima.
    private static final Instant MIERCOLES = Instant.parse("2026-10-14T15:00:00Z");
    private static final LocalDate MIERCOLES_LOCAL = LocalDate.of(2026, 10, 14);

    private CuestionarioConsultaService consulta;
    private SesionesCheckin sesiones;

    private static CuestionarioPendienteResponse cuestionario(long id, int cadenciaDias) {
        return new CuestionarioPendienteResponse(id, "C" + id, "Cuestionario " + id, cadenciaDias, null, List.of());
    }

    private static String hash(Long persona, long cuestionarioId, int cadenciaDias, Instant instante) {
        LocalDate ventana = VentanaSemanal.inicioDeVentana(instante.atZone(ZoneOffset.ofHours(-5)).toLocalDate(), cadenciaDias);
        return SesionHasher.hashDeSesion(SECRETO, persona, cuestionarioId, ventana);
    }

    private CuestionariosPorResponderService servicio(Instant ahora) {
        return new CuestionariosPorResponderService(consulta, sesiones, SECRETO, ZONA, Clock.fixed(ahora, ZoneOffset.UTC));
    }

    @BeforeEach
    void setUp() {
        consulta = mock(CuestionarioConsultaService.class);
        sesiones = mock(SesionesCheckin.class);
    }

    @Test
    void sinSesionGuardada_elCuestionarioSigueSiendoPendiente() {
        when(consulta.pendientes(EMPRESA)).thenReturn(List.of(cuestionario(10, 7)));
        when(sesiones.existe(anyString())).thenReturn(false);

        assertThat(servicio(MIERCOLES).porResponder(EMPRESA, PERSONA)).extracting(CuestionarioPendienteResponse::id).containsExactly(10L);
    }

    @Test
    void conSesionGuardadaEnLaVentanaActual_elCuestionarioYaNoEsPendiente() {
        when(consulta.pendientes(EMPRESA)).thenReturn(List.of(cuestionario(10, 7)));
        when(sesiones.existe(hash(PERSONA, 10, 7, MIERCOLES))).thenReturn(true);

        assertThat(servicio(MIERCOLES).porResponder(EMPRESA, PERSONA)).isEmpty();
    }

    @Test
    void soloSeOcultaElCuestionarioRespondido_losDemasSiguenPendientes() {
        when(consulta.pendientes(EMPRESA)).thenReturn(List.of(cuestionario(10, 7), cuestionario(11, 14)));
        when(sesiones.existe(hash(PERSONA, 10, 7, MIERCOLES))).thenReturn(true);

        assertThat(servicio(MIERCOLES).porResponder(EMPRESA, PERSONA)).extracting(CuestionarioPendienteResponse::id).containsExactly(11L);
    }

    @Test
    void laSesionDeOtraPersonaNoOcultaElCuestionario() {
        when(consulta.pendientes(EMPRESA)).thenReturn(List.of(cuestionario(10, 7)));
        when(sesiones.existe(hash(8L, 10, 7, MIERCOLES))).thenReturn(true);

        assertThat(servicio(MIERCOLES).porResponder(EMPRESA, PERSONA)).hasSize(1);
    }

    @Test
    void laVentanaSeCuentaConLaCadenciaDelCuestionario() {
        when(consulta.pendientes(EMPRESA)).thenReturn(List.of(cuestionario(11, 14)));
        // Con cadencia de 14 días la ventana no es la semana: se consulta el hash del bloque de 14 días.
        LocalDate bloque = VentanaSemanal.inicioDeVentana(MIERCOLES_LOCAL, 14);
        when(sesiones.existe(SesionHasher.hashDeSesion(SECRETO, PERSONA, 11L, bloque))).thenReturn(true);

        assertThat(servicio(MIERCOLES).porResponder(EMPRESA, PERSONA)).isEmpty();
    }

    @Test
    void alEmpezarLaSiguienteVentana_elCuestionarioVuelveASerPendiente() {
        when(consulta.pendientes(EMPRESA)).thenReturn(List.of(cuestionario(10, 7)));
        when(sesiones.existe(hash(PERSONA, 10, 7, MIERCOLES))).thenReturn(true);
        Instant semanaSiguiente = MIERCOLES.plusSeconds(7 * 24 * 3600L);

        assertThat(servicio(semanaSiguiente).porResponder(EMPRESA, PERSONA)).hasSize(1);
    }

    @Test
    void sinCuestionariosActivos_devuelveUnaListaVacia() {
        when(consulta.pendientes(EMPRESA)).thenReturn(List.of());

        assertThat(servicio(MIERCOLES).porResponder(EMPRESA, PERSONA)).isEmpty();
    }
}
