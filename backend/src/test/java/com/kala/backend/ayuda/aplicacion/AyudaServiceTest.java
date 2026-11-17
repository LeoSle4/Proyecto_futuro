package com.kala.backend.ayuda.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kala.backend.ayuda.aplicacion.puerto.ContactosAyuda;
import com.kala.backend.ayuda.aplicacion.puerto.SolicitudesAyuda;
import com.kala.backend.ayuda.dominio.ContactoAyuda;
import com.kala.backend.ayuda.dominio.ContactoAyudaNoEncontradoException;
import com.kala.backend.ayuda.dominio.SolicitudAyuda;
import com.kala.backend.ayuda.dominio.SolicitudAyudaEstadoException;
import com.kala.backend.ayuda.dominio.SolicitudAyudaNoEncontradaException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

class AyudaServiceTest {

    private static final Long EMPRESA = 1L;
    private static final Long EMPLEADO = 11L;
    private static final Long COORDINADOR = 21L;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-17T15:00:00Z"), ZoneOffset.UTC);

    private ContactosAyuda contactos;
    private SolicitudesAyuda solicitudes;
    private AyudaService service;

    @BeforeEach
    void setUp() {
        contactos = mock(ContactosAyuda.class);
        solicitudes = mock(SolicitudesAyuda.class);
        when(contactos.guardar(any())).thenAnswer(this::primerArgumento);
        when(solicitudes.guardar(any())).thenAnswer(this::primerArgumento);
        service = new AyudaService(contactos, solicitudes, RELOJ);
    }

    private Object primerArgumento(InvocationOnMock invocacion) {
        return invocacion.getArgument(0);
    }

    // ---- contactos ----

    @Test
    void crearContacto_guardaloActivoEnLaEmpresaDelToken() {
        ContactoAyuda c = service.crearContacto(EMPRESA, "Dra. Ruiz", "PSICOLOGO", "999111222", null);

        assertThat(c.getEmpresaId()).isEqualTo(EMPRESA);
        assertThat(c.isActivo()).isTrue();
        assertThat(c.getTipo()).isEqualTo("PSICOLOGO");
    }

    @Test
    void crearContacto_conTipoDesconocido_esInvalido() {
        assertThatThrownBy(() -> service.crearContacto(EMPRESA, "X", "BRUJO", "999", null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(contactos, never()).guardar(any());
    }

    @Test
    void crearContacto_sinTelefonoNiEmail_esInvalido() {
        assertThatThrownBy(() -> service.crearContacto(EMPRESA, "X", "OTRO", " ", null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(contactos, never()).guardar(any());
    }

    @Test
    void desactivarContacto_deOtraEmpresa_es404() {
        when(contactos.buscarDeEmpresa(5L, EMPRESA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desactivarContacto(EMPRESA, 5L))
                .isInstanceOf(ContactoAyudaNoEncontradoException.class);
    }

    @Test
    void desactivarContacto_loMarcaInactivo() {
        ContactoAyuda c = new ContactoAyuda(EMPRESA, "Linea", "LINEA_CRISIS", "113", null);
        when(contactos.buscarDeEmpresa(5L, EMPRESA)).thenReturn(Optional.of(c));

        service.desactivarContacto(EMPRESA, 5L);

        assertThat(c.isActivo()).isFalse();
    }

    @Test
    void elEmpleadoSoloVeContactosActivos_yElPersonalTodos() {
        when(contactos.activosDeEmpresa(EMPRESA)).thenReturn(List.of());
        when(contactos.deEmpresa(EMPRESA)).thenReturn(List.of());

        service.listarContactos(EMPRESA, true);
        service.listarContactos(EMPRESA, false);

        verify(contactos).activosDeEmpresa(EMPRESA);
        verify(contactos).deEmpresa(EMPRESA);
    }

    // ---- solicitudes ----

    @Test
    void solicitar_naceEnPendienteYGuardaElMensaje() {
        SolicitudAyuda s = service.solicitar(EMPLEADO, "Necesito hablar con alguien");

        assertThat(s.getUsuarioId()).isEqualTo(EMPLEADO);
        assertThat(s.getEstado()).isEqualTo(SolicitudAyuda.PENDIENTE);
        assertThat(s.getMensaje()).isEqualTo("Necesito hablar con alguien");
        assertThat(s.getContactoId()).isNull();
    }

    @Test
    void atender_asignaContactoYPersonal_yPasaAEnAtencion() {
        SolicitudAyuda s = new SolicitudAyuda(EMPLEADO, null);
        when(solicitudes.buscarDeEmpresa(7L, EMPRESA)).thenReturn(Optional.of(s));
        when(contactos.buscarDeEmpresa(5L, EMPRESA))
                .thenReturn(Optional.of(new ContactoAyuda(EMPRESA, "Dra. Ruiz", "PSICOLOGO", "999", null)));

        SolicitudAyuda r = service.atender(EMPRESA, COORDINADOR, 7L, 5L);

        assertThat(r.getEstado()).isEqualTo(SolicitudAyuda.EN_ATENCION);
        assertThat(r.getAtendidoPor()).isEqualTo(COORDINADOR);
        assertThat(r.getContactoId()).isEqualTo(5L);
        assertThat(r.getAtendidoEn()).isEqualTo(LocalDateTime.of(2026, 11, 17, 15, 0));
    }

    @Test
    void atender_sinContacto_tambienEsValido() {
        SolicitudAyuda s = new SolicitudAyuda(EMPLEADO, null);
        when(solicitudes.buscarDeEmpresa(7L, EMPRESA)).thenReturn(Optional.of(s));

        SolicitudAyuda r = service.atender(EMPRESA, COORDINADOR, 7L, null);

        assertThat(r.getEstado()).isEqualTo(SolicitudAyuda.EN_ATENCION);
        assertThat(r.getContactoId()).isNull();
    }

    @Test
    void atender_unaSolicitudDeOtraEmpresa_es404() {
        when(solicitudes.buscarDeEmpresa(7L, EMPRESA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atender(EMPRESA, COORDINADOR, 7L, null))
                .isInstanceOf(SolicitudAyudaNoEncontradaException.class);
        verify(solicitudes, never()).guardar(any());
    }

    @Test
    void atender_conUnContactoInactivoOAjeno_es404YNoCambiaLaSolicitud() {
        SolicitudAyuda s = new SolicitudAyuda(EMPLEADO, null);
        ContactoAyuda inactivo = new ContactoAyuda(EMPRESA, "Baja", "OTRO", "1", null);
        inactivo.desactivar();
        when(solicitudes.buscarDeEmpresa(7L, EMPRESA)).thenReturn(Optional.of(s));
        when(contactos.buscarDeEmpresa(5L, EMPRESA)).thenReturn(Optional.of(inactivo));
        when(contactos.buscarDeEmpresa(6L, EMPRESA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atender(EMPRESA, COORDINADOR, 7L, 5L))
                .isInstanceOf(ContactoAyudaNoEncontradoException.class);
        assertThatThrownBy(() -> service.atender(EMPRESA, COORDINADOR, 7L, 6L))
                .isInstanceOf(ContactoAyudaNoEncontradoException.class);
        assertThat(s.getEstado()).isEqualTo(SolicitudAyuda.PENDIENTE);
    }

    @Test
    void atender_unaSolicitudYaEnAtencion_es409() {
        SolicitudAyuda s = new SolicitudAyuda(EMPLEADO, null);
        s.atender(COORDINADOR, null, LocalDateTime.now());
        when(solicitudes.buscarDeEmpresa(7L, EMPRESA)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.atender(EMPRESA, COORDINADOR, 7L, null))
                .isInstanceOf(SolicitudAyudaEstadoException.class);
    }

    @Test
    void cerrar_desdeEnAtencion_pasaACerrada() {
        SolicitudAyuda s = new SolicitudAyuda(EMPLEADO, null);
        s.atender(COORDINADOR, null, LocalDateTime.now());
        when(solicitudes.buscarDeEmpresa(7L, EMPRESA)).thenReturn(Optional.of(s));

        assertThat(service.cerrar(EMPRESA, COORDINADOR, 7L).getEstado()).isEqualTo(SolicitudAyuda.CERRADA);
    }

    @Test
    void cerrar_unaSolicitudYaCerrada_es409() {
        SolicitudAyuda s = new SolicitudAyuda(EMPLEADO, null);
        s.atender(COORDINADOR, null, LocalDateTime.now());
        s.cerrar(COORDINADOR, LocalDateTime.now());
        when(solicitudes.buscarDeEmpresa(7L, EMPRESA)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.cerrar(EMPRESA, COORDINADOR, 7L))
                .isInstanceOf(SolicitudAyudaEstadoException.class);
    }

    @Test
    void solicitudesDeEmpresa_filtraPorEstadoSiSeIndica() {
        SolicitudAyuda pendiente = new SolicitudAyuda(EMPLEADO, null);
        SolicitudAyuda cerrada = new SolicitudAyuda(12L, null);
        cerrada.atender(COORDINADOR, null, LocalDateTime.now());
        cerrada.cerrar(COORDINADOR, LocalDateTime.now());
        when(solicitudes.deEmpresa(EMPRESA)).thenReturn(List.of(pendiente, cerrada));

        assertThat(service.solicitudesDeEmpresa(EMPRESA, null)).hasSize(2);
        assertThat(service.solicitudesDeEmpresa(EMPRESA, "PENDIENTE")).containsExactly(pendiente);
    }
}
