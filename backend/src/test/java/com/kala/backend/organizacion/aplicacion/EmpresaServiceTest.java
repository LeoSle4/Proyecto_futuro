

package com.kala.backend.organizacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kala.backend.organizacion.aplicacion.puerto.Empresas;
import com.kala.backend.organizacion.aplicacion.puerto.Equipos;
import com.kala.backend.organizacion.dominio.Empresa;
import com.kala.backend.organizacion.dominio.EmpresaConEquiposException;
import com.kala.backend.organizacion.dominio.EmpresaNoEncontradaException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmpresaServiceTest {

    @Mock
    private Empresas empresaRepository;

    @Mock
    private Equipos equipoRepository;

    private EmpresaService service;

    @BeforeEach
    void setUp() {
        service = new EmpresaService(empresaRepository, equipoRepository);
    }

    @Test
    void listar_devuelveEmpresasDelRepositorio() {
        List<Empresa> empresas = List.of(
                new Empresa(1L, "ACME", "Tecnologia")
        );

        empresas.get(0).setRuc("20123456789");

        when(empresaRepository.todas()).thenReturn(empresas);

        assertThat(service.listar()).isEqualTo(empresas);
    }

    @Test
    void buscarPorNombre_conEmpresaIdConsultaSoloEsaEmpresa() {
        Empresa empresa = new Empresa(2L, "Beta", "Servicios");
        when(empresaRepository.buscarPorNombreEnEmpresa(2L, "beta"))
                .thenReturn(List.of(empresa));

        assertThat(service.buscarPorNombre(" beta ", 2L))
                .containsExactly(empresa);

        verify(empresaRepository).buscarPorNombreEnEmpresa(2L, "beta");
    }

    @Test
    void crearEmpresaValida_guardaLaEmpresa() {
        EmpresaRequest request =
                new EmpresaRequest("20123456789", "ACME", "Tecnologia");

        when(empresaRepository.guardar(any(Empresa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Empresa creada = service.crear(request);

        assertThat(creada.getRuc()).isEqualTo("20123456789");
        assertThat(creada.getNombre()).isEqualTo("ACME");
        assertThat(creada.getRubro()).isEqualTo("Tecnologia");

        verify(empresaRepository).guardar(any(Empresa.class));
    }

    @Test
    void buscarPorIdInexistente_lanzaEmpresaNoEncontradaException() {
        when(empresaRepository.porId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(999L))
                .isInstanceOf(EmpresaNoEncontradaException.class)
                .hasMessageContaining("999");
    }

    @Test
    void actualizarEmpresaExistente_guardaLosNuevosValores() {
        Empresa existente =
                new Empresa(1L, "ACME", "Tecnologia");
        existente.setRuc("20123456789");

        EmpresaRequest request =
                new EmpresaRequest("20987654321", "ACME Corp", "Software");

        when(empresaRepository.porId(1L))
                .thenReturn(Optional.of(existente));
        when(empresaRepository.guardar(existente)).thenReturn(existente);

        Empresa actualizada = service.actualizar(1L, request);

        assertThat(actualizada.getId()).isEqualTo(1L);
        assertThat(actualizada.getRuc()).isEqualTo("20987654321");
        assertThat(actualizada.getNombre()).isEqualTo("ACME Corp");
        assertThat(actualizada.getRubro()).isEqualTo("Software");

        verify(empresaRepository).guardar(existente);
    }

    @Test
    void eliminarEmpresaInexistente_lanzaEmpresaNoEncontradaException() {
        when(empresaRepository.porId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(999L))
                .isInstanceOf(EmpresaNoEncontradaException.class);
    }

    @Test
    void eliminarEmpresaConEquipos_lanzaConflicto() {
        Empresa empresa =
                new Empresa(1L, "ACME", "Tecnologia");
        empresa.setRuc("20123456789");

        when(empresaRepository.porId(1L))
                .thenReturn(Optional.of(empresa));
        when(equipoRepository.tieneEquipos(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(1L))
                .isInstanceOf(EmpresaConEquiposException.class)
                .hasMessageContaining("equipos asociados");
    }
}
