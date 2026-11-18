package com.kala.backend.instrumentos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kala.backend.instrumentos.aplicacion.CuestionarioConsultaService.CuestionarioConEstado;
import com.kala.backend.instrumentos.aplicacion.puerto.ConfiguracionesLdp;
import com.kala.backend.instrumentos.aplicacion.puerto.Cuestionarios;
import com.kala.backend.instrumentos.aplicacion.puerto.EscalasRespuesta;
import com.kala.backend.instrumentos.aplicacion.puerto.ItemsDeCuestionario;
import com.kala.backend.instrumentos.aplicacion.puerto.OpcionesEscala;
import com.kala.backend.instrumentos.dominio.Cuestionario;
import com.kala.backend.organizacion.aplicacion.puerto.Empresas;
import com.kala.backend.organizacion.dominio.EmpresaNoEncontradaException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CuestionarioConsultaServiceTest {

    private Empresas empresas;
    private Cuestionarios cuestionarios;
    private CuestionarioConsultaService service;

    @BeforeEach
    void setUp() {
        empresas = mock(Empresas.class);
        cuestionarios = mock(Cuestionarios.class);
        service = new CuestionarioConsultaService(empresas, cuestionarios, mock(ItemsDeCuestionario.class),
                mock(EscalasRespuesta.class), mock(OpcionesEscala.class), mock(ConfiguracionesLdp.class));
    }

    @Test
    void listarConEstado_marcaActivosLosQueLaEmpresaTieneYLosDemasInactivos() {
        Cuestionario pulso = cuestionario(1L, "PULSO");
        Cuestionario who5 = cuestionario(2L, "WHO5");
        Cuestionario phq8 = cuestionario(3L, "PHQ8");
        when(empresas.existe(4L)).thenReturn(true);
        when(cuestionarios.todos()).thenReturn(List.of(pulso, who5, phq8));
        when(cuestionarios.activosDeEmpresa(4L)).thenReturn(List.of(pulso, phq8));

        List<CuestionarioConEstado> lista = service.listarConEstado(4L);

        assertThat(lista).extracting(c -> c.cuestionario().getCodigo()).containsExactly("PULSO", "WHO5", "PHQ8");
        assertThat(lista).extracting(CuestionarioConEstado::activo).containsExactly(true, false, true);
    }

    @Test
    void listarConEstado_conUnaEmpresaQueNoExisteFalla() {
        when(empresas.existe(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.listarConEstado(99L)).isInstanceOf(EmpresaNoEncontradaException.class);
    }

    private static Cuestionario cuestionario(Long id, String codigo) {
        Cuestionario c = new Cuestionario(codigo, codigo, (short) 7, null);
        org.springframework.test.util.ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
