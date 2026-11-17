
package com.kala.backend.organizacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kala.backend.organizacion.aplicacion.puerto.Equipos;
import com.kala.backend.organizacion.dominio.Empresa;
import com.kala.backend.organizacion.dominio.EmpresaNoEncontradaException;
import com.kala.backend.organizacion.dominio.Equipo;
import com.kala.backend.organizacion.dominio.EquipoNoEncontradoException;
import com.kala.backend.organizacion.infraestructura.persistencia.EmpresaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EquipoServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private Equipos equipoRepository;

    private EmpresaService empresaService;
    private EquipoService service;

    private final Long empresaId = 1L;

    @BeforeEach
    void setUp() {
        empresaService = new EmpresaService(empresaRepository, equipoRepository);
        service = new EquipoService(equipoRepository, empresaService);
    }

    private Empresa crearEmpresa() {
        Empresa empresa = new Empresa(empresaId, "ACME", "Tecnologia");
        empresa.setRuc("20123456789");
        return empresa;
    }

    @Test
    void crearEquipoValido_guardaEquipoConEmpresaId() {
        when(empresaRepository.porId(empresaId))
                .thenReturn(Optional.of(crearEmpresa()));
        when(equipoRepository.guardar(any(Equipo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Equipo creado = service.crear(
                new EquipoRequest("Equipo Ventas", empresaId)
        );

        assertThat(creado.getNombre()).isEqualTo("Equipo Ventas");
        assertThat(creado.getEmpresaId()).isEqualTo(empresaId);
        verify(equipoRepository).guardar(any(Equipo.class));
    }

    @Test
    void crearEquipoConEmpresaInexistente_lanzaEmpresaNoEncontradaException() {
        when(empresaRepository.porId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.crear(new EquipoRequest("Equipo huérfano", 999L)))
                .isInstanceOf(EmpresaNoEncontradaException.class);
    }

    @Test
    void crearEquipoConEmpresaIdNulo_lanzaIllegalArgumentException() {
        assertThatThrownBy(() ->
                service.crear(new EquipoRequest("Sin empresa", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empresaId");
    }

    @Test
    void listarPorEmpresa_filtraPorEmpresaId() {
        List<Equipo> equipos = List.of(
                new Equipo(1L, "Equipo A", empresaId)
        );

        when(equipoRepository.deEmpresa(empresaId)).thenReturn(equipos);

        assertThat(service.listarPorEmpresa(empresaId))
                .extracting(Equipo::getNombre)
                .containsExactly("Equipo A");

        verify(equipoRepository).deEmpresa(empresaId);
    }

    @Test
    void listarPorEmpresaSinFiltro_devuelveTodosLosEquipos() {
        List<Equipo> equipos = List.of(
                new Equipo(1L, "Equipo A", empresaId),
                new Equipo(2L, "Equipo B", 2L)
        );

        when(equipoRepository.todos()).thenReturn(equipos);

        assertThat(service.listarPorEmpresa(null)).hasSize(2);
    }

    @Test
    void buscarPorIdInexistente_lanzaEquipoNoEncontradoException() {
        when(equipoRepository.porId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(999L))
                .isInstanceOf(EquipoNoEncontradoException.class)
                .hasMessageContaining("999");
    }

    @Test
    void buscarPorIdEnEmpresa_consultaConIdYEmpresaId() {
        Equipo equipo = new Equipo(5L, "Ventas", 2L);
        when(equipoRepository.porIdEnEmpresa(5L, 2L))
                .thenReturn(Optional.of(equipo));

        assertThat(service.buscarPorIdEnEmpresa(5L, 2L))
                .isSameAs(equipo);

        verify(equipoRepository).porIdEnEmpresa(5L, 2L);
    }

    @Test
    void buscarPorIdEnEmpresa_deOtraEmpresa_lanza404() {
        when(equipoRepository.porIdEnEmpresa(5L, empresaId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorIdEnEmpresa(5L, empresaId))
                .isInstanceOf(EquipoNoEncontradoException.class);
    }

    @Test
    void actualizarEnEmpresa_soloActualizaElEquipoDeLaEmpresaIndicada() {
        Equipo equipo = new Equipo(5L, "Ventas", empresaId);
        when(empresaRepository.porId(empresaId))
                .thenReturn(Optional.of(crearEmpresa()));
        when(equipoRepository.porIdEnEmpresa(5L, empresaId))
                .thenReturn(Optional.of(equipo));
        when(equipoRepository.guardar(equipo)).thenReturn(equipo);

        Equipo actualizado = service.actualizarEnEmpresa(
                5L, new EquipoRequest("Marketing", empresaId), empresaId);

        assertThat(actualizado.getNombre()).isEqualTo("Marketing");
        assertThat(actualizado.getEmpresaId()).isEqualTo(empresaId);
        verify(equipoRepository).porIdEnEmpresa(5L, empresaId);
        verify(equipoRepository).guardar(equipo);
    }

    @Test
    void eliminarEnEmpresa_soloEliminaElEquipoDeLaEmpresaIndicada() {
        Equipo equipo = new Equipo(5L, "Ventas", 2L);
        when(equipoRepository.porIdEnEmpresa(5L, 2L))
                .thenReturn(Optional.of(equipo));

        service.eliminarEnEmpresa(5L, 2L);

        verify(equipoRepository).porIdEnEmpresa(5L, 2L);
        verify(equipoRepository).eliminar(equipo);
    }

    @Test
    void actualizarEquipoExistente_guardaLosNuevosValores() {
        Equipo existente = new Equipo(5L, "Ventas", empresaId);

        when(empresaRepository.porId(empresaId))
                .thenReturn(Optional.of(crearEmpresa()));
        when(equipoRepository.porId(5L))
                .thenReturn(Optional.of(existente));
        when(equipoRepository.guardar(existente)).thenReturn(existente);

        Equipo actualizado = service.actualizar(
                5L, new EquipoRequest("Marketing", empresaId)
        );

        assertThat(actualizado.getNombre()).isEqualTo("Marketing");
        verify(equipoRepository).guardar(existente);
    }

    @Test
    void eliminarEquipoInexistente_lanzaEquipoNoEncontradoException() {
        when(equipoRepository.porId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(999L))
                .isInstanceOf(EquipoNoEncontradoException.class);
    }
}
