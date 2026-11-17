package com.kala.backend.instrumentos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kala.backend.instrumentos.aplicacion.puerto.Activaciones;
import com.kala.backend.instrumentos.aplicacion.puerto.ConfiguracionesLdp;
import com.kala.backend.instrumentos.aplicacion.puerto.Cuestionarios;
import com.kala.backend.instrumentos.aplicacion.puerto.ItemsDeCuestionario;
import com.kala.backend.instrumentos.aplicacion.puerto.Presupuestos;
import com.kala.backend.instrumentos.dominio.ConfiguracionLdp;
import com.kala.backend.instrumentos.dominio.Cuestionario;
import com.kala.backend.instrumentos.dominio.CuestionarioNoEncontradoException;
import com.kala.backend.instrumentos.dominio.EmpresaCuestionario;
import com.kala.backend.instrumentos.dominio.ItemCuestionario;
import com.kala.backend.instrumentos.dominio.PresupuestoExcedidoException;
import com.kala.backend.instrumentos.dominio.PresupuestoPrivacidad;
import com.kala.backend.organizacion.aplicacion.puerto.Empresas;
import com.kala.backend.organizacion.dominio.EmpresaNoEncontradaException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ActivacionCuestionarioServiceTest {

    private static final Long EMPRESA = 1L;

    private final Empresas empresas = mock(Empresas.class);
    private final Cuestionarios cuestionarios = mock(Cuestionarios.class);
    private final ItemsDeCuestionario items = mock(ItemsDeCuestionario.class);
    private final ConfiguracionesLdp configuraciones = mock(ConfiguracionesLdp.class);
    private final Activaciones activaciones = mock(Activaciones.class);
    private final Presupuestos presupuestos = mock(Presupuestos.class);
    // 15 de marzo de 2026, hora de Lima
    private final Clock reloj = Clock.fixed(Instant.parse("2026-03-15T15:00:00Z"), ZoneId.of("America/Lima"));

    private ActivacionCuestionarioService service;

    @BeforeEach
    void preparar() {
        service = new ActivacionCuestionarioService(empresas, cuestionarios, items, configuraciones,
                activaciones, presupuestos, reloj, 20.0);
        when(empresas.existe(EMPRESA)).thenReturn(true);
        when(cuestionarios.activosDeEmpresa(EMPRESA)).thenReturn(List.of());
        when(activaciones.porId(any(), any())).thenReturn(Optional.empty());
        when(presupuestos.deEmpresaYPeriodo(any(), any())).thenReturn(Optional.empty());
    }

    // Cuestionario con un solo ítem de Randomized Response con p = 0.75 y la cadencia indicada.
    private Cuestionario cuestionarioConUnItem(Long id, int cadenciaDias) {
        Cuestionario c = new Cuestionario("C" + id, "Cuestionario " + id, (short) cadenciaDias, null);
        ReflectionTestUtils.setField(c, "id", id);
        ItemCuestionario item = new ItemCuestionario(id, (short) 1, "Tema", "Texto", 1L, 7L);
        ReflectionTestUtils.setField(item, "id", id * 10);
        when(cuestionarios.porId(id)).thenReturn(Optional.of(c));
        when(items.deCuestionario(id)).thenReturn(List.of(item));
        when(configuraciones.porId(7L)).thenReturn(
                Optional.of(ConfiguracionLdp.randomizedResponse(new BigDecimal("0.750"))));
        return c;
    }

    @Test
    void dentroDelLimite_activaYGuarda() {
        cuestionarioConUnItem(10L, 7);

        var resultado = service.activar(EMPRESA, 10L);

        assertThat(resultado.epsilonMensual()).isCloseTo(5.49, org.assertj.core.data.Offset.offset(0.01));
        assertThat(resultado.epsilonMaximo()).isEqualTo(20.0);
        verify(activaciones).guardar(any(EmpresaCuestionario.class));
    }

    @Test
    void siExcedeElMaximo_lanza422YNoGuardaNada() {
        cuestionarioConUnItem(10L, 1);

        assertThatThrownBy(() -> service.activar(EMPRESA, 10L))
                .isInstanceOf(PresupuestoExcedidoException.class);
        verify(activaciones, never()).guardar(any());
    }

    @Test
    void sumaLoYaActivoConLoNuevo() {
        Cuestionario activo = cuestionarioConUnItem(10L, 7);
        when(cuestionarios.activosDeEmpresa(EMPRESA)).thenReturn(List.of(activo));
        cuestionarioConUnItem(11L, 7);

        var resultado = service.activar(EMPRESA, 11L);

        assertThat(resultado.epsilonMensual()).isCloseTo(10.99, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void reactivarElMismoCuestionario_noDuplicaElGasto() {
        Cuestionario activo = cuestionarioConUnItem(10L, 7);
        when(cuestionarios.activosDeEmpresa(EMPRESA)).thenReturn(List.of(activo));

        var resultado = service.activar(EMPRESA, 10L);

        assertThat(resultado.epsilonMensual()).isCloseTo(5.49, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void laFilaDePresupuestoDelMesTienePrioridadSobreElValorPorDefecto() {
        cuestionarioConUnItem(10L, 7);
        when(presupuestos.deEmpresaYPeriodo(EMPRESA, "2026-03"))
                .thenReturn(Optional.of(new PresupuestoPrivacidad(EMPRESA, "2026-03", new BigDecimal("5.000"))));

        assertThatThrownBy(() -> service.activar(EMPRESA, 10L))
                .isInstanceOf(PresupuestoExcedidoException.class);
        verify(activaciones, never()).guardar(any());
    }

    @Test
    void empresaOCuestionarioInexistente_son404() {
        when(empresas.existe(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.activar(99L, 10L)).isInstanceOf(EmpresaNoEncontradaException.class);

        when(cuestionarios.porId(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.activar(EMPRESA, 5L)).isInstanceOf(CuestionarioNoEncontradoException.class);
        verify(activaciones, never()).guardar(any());
    }
}
