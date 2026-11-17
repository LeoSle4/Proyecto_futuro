package com.kala.backend.identidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kala.backend.identidad.aplicacion.puerto.Empleados;
import com.kala.backend.identidad.aplicacion.puerto.HasherDeClaves;
import com.kala.backend.identidad.aplicacion.puerto.PersonalDeEmpresa;
import com.kala.backend.identidad.aplicacion.puerto.Usuarios;
import com.kala.backend.identidad.dominio.CredencialesInvalidasException;
import com.kala.backend.identidad.dominio.EmailEnUsoException;
import com.kala.backend.identidad.dominio.EmpleadoEquipo;
import com.kala.backend.identidad.dominio.PersonalEmpresa;
import com.kala.backend.identidad.dominio.RolPersonal;
import com.kala.backend.identidad.dominio.Usuario;
import com.kala.backend.identidad.dominio.UsuarioNoEncontradoException;
import com.kala.backend.organizacion.aplicacion.puerto.Empresas;
import com.kala.backend.organizacion.aplicacion.puerto.Equipos;
import com.kala.backend.organizacion.dominio.Empresa;
import com.kala.backend.organizacion.dominio.EmpresaNoEncontradaException;
import com.kala.backend.organizacion.dominio.Equipo;
import com.kala.backend.organizacion.dominio.EquipoNoEncontradoException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;

class GestionUsuariosServiceTest {

    private static final String CLAVE = "una-clave-larga-123";

    private Usuarios usuarios;
    private Empleados empleados;
    private PersonalDeEmpresa personal;
    private HasherDeClaves hasher;
    private Empresas empresas;
    private Equipos equipos;
    private GestionUsuariosService service;

    @BeforeEach
    void setUp() {
        usuarios = mock(Usuarios.class);
        empleados = mock(Empleados.class);
        personal = mock(PersonalDeEmpresa.class);
        hasher = mock(HasherDeClaves.class);
        empresas = mock(Empresas.class);
        equipos = mock(Equipos.class);
        when(hasher.cifrar(any())).thenAnswer((InvocationOnMock i) -> "hash(" + i.getArgument(0) + ")");
        when(usuarios.guardar(any())).thenAnswer(i -> i.getArgument(0));
        service = new GestionUsuariosService(usuarios, empleados, personal, hasher, empresas, equipos);
    }

    // ---- personal de empresa (lo crea el ADMIN) ----

    @Test
    void crearPersonal_guardaElUsuarioConLaClaveCifradaYElSubtipo() {
        when(empresas.existe(4L)).thenReturn(true);

        Usuario u = service.crearPersonal(4L, "  Ana@Kala.TEST ", "Ana Ríos", CLAVE, RolPersonal.RRHH);

        assertThat(u.getEmail()).isEqualTo("ana@kala.test");
        assertThat(u.getNombre()).isEqualTo("Ana Ríos");
        assertThat(u.getPasswordHash()).isEqualTo("hash(" + CLAVE + ")").doesNotContain("Ana");
        ArgumentCaptor<PersonalEmpresa> sub = ArgumentCaptor.forClass(PersonalEmpresa.class);
        verify(personal).guardar(sub.capture());
        assertThat(sub.getValue().getEmpresaId()).isEqualTo(4L);
        assertThat(sub.getValue().getRol()).isEqualTo(RolPersonal.RRHH);
    }

    @Test
    void crearPersonal_enUnaEmpresaInexistente_es404YNoGuardaNada() {
        when(empresas.existe(4L)).thenReturn(false);

        assertThatThrownBy(() -> service.crearPersonal(4L, "a@kala.test", "Ana", CLAVE, RolPersonal.RRHH))
                .isInstanceOf(EmpresaNoEncontradaException.class);

        verify(usuarios, never()).guardar(any());
        verifyNoInteractions(personal);
    }

    @Test
    void crearPersonal_conUnCorreoYaRegistrado_es409() {
        when(empresas.existe(4L)).thenReturn(true);
        when(usuarios.existeEmail("ana@kala.test")).thenReturn(true);

        assertThatThrownBy(() -> service.crearPersonal(4L, "ANA@kala.test", "Ana", CLAVE, RolPersonal.RRHH))
                .isInstanceOf(EmailEnUsoException.class);

        verify(usuarios, never()).guardar(any());
    }

    // ---- empleados (los crea RRHH o el coordinador de su empresa) ----

    @Test
    void crearEmpleado_enUnEquipoDeSuEmpresa_guardaUsuarioYSubtipo() {
        when(equipos.porIdEnEmpresa(9L, 4L)).thenReturn(Optional.of(new Equipo("Ventas", 4L)));
        when(usuarios.guardar(any())).thenAnswer(i -> i.getArgument(0));

        Usuario u = service.crearEmpleado(4L, 9L, "luis@kala.test", "Luis", CLAVE);

        assertThat(u.getEmail()).isEqualTo("luis@kala.test");
        ArgumentCaptor<EmpleadoEquipo> sub = ArgumentCaptor.forClass(EmpleadoEquipo.class);
        verify(empleados).guardar(sub.capture());
        assertThat(sub.getValue().getEquipoId()).isEqualTo(9L);
    }

    @Test
    void crearEmpleado_enUnEquipoDeOtraEmpresa_es404YNoGuardaNada() {
        when(equipos.porIdEnEmpresa(9L, 4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearEmpleado(4L, 9L, "luis@kala.test", "Luis", CLAVE))
                .isInstanceOf(EquipoNoEncontradoException.class);

        verify(usuarios, never()).guardar(any());
        verifyNoInteractions(empleados);
    }

    // ---- reglas de las credenciales ----

    @Test
    void laClaveDebeTenerEntre12Y72Caracteres() {
        when(empresas.existe(4L)).thenReturn(true);

        assertThatThrownBy(() -> service.crearPersonal(4L, "a@kala.test", "Ana", "corta", RolPersonal.RRHH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.crearPersonal(4L, "a@kala.test", "Ana", "x".repeat(73), RolPersonal.RRHH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.crearPersonal(4L, "a@kala.test", "Ana", null, RolPersonal.RRHH))
                .isInstanceOf(IllegalArgumentException.class);
        verify(usuarios, never()).guardar(any());
    }

    @Test
    void elMensajeDeUnaClaveInvalidaNuncaIncluyeLaClave() {
        assertThatThrownBy(() -> service.crearPersonal(4L, "a@kala.test", "Ana", "secreta", RolPersonal.RRHH))
                .hasMessageNotContaining("secreta");
    }

    @Test
    void elCorreoDebeSerValido_yElNombreObligatorio() {
        when(empresas.existe(4L)).thenReturn(true);

        assertThatThrownBy(() -> service.crearPersonal(4L, "sin-arroba", "Ana", CLAVE, RolPersonal.RRHH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.crearPersonal(4L, "a@kala.test", "  ", CLAVE, RolPersonal.RRHH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- cambio de la propia clave ----

    @Test
    void cambiarClave_conLaActualCorrecta_guardaElNuevoHash() {
        Usuario u = new Usuario("ana@kala.test", "hash-viejo", "Ana");
        when(usuarios.porId(7L)).thenReturn(Optional.of(u));
        when(hasher.coincide("la-clave-actual-1", "hash-viejo")).thenReturn(true);

        service.cambiarClave(7L, "la-clave-actual-1", "otra-clave-nueva-2");

        assertThat(u.getPasswordHash()).isEqualTo("hash(otra-clave-nueva-2)");
        verify(usuarios).guardar(u);
    }

    @Test
    void cambiarClave_conLaActualIncorrecta_es401YNoCambiaNada() {
        Usuario u = new Usuario("ana@kala.test", "hash-viejo", "Ana");
        when(usuarios.porId(7L)).thenReturn(Optional.of(u));
        when(hasher.coincide(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.cambiarClave(7L, "equivocada-123456", "otra-clave-nueva-2"))
                .isInstanceOf(CredencialesInvalidasException.class);

        assertThat(u.getPasswordHash()).isEqualTo("hash-viejo");
        verify(usuarios, never()).guardar(any());
    }

    @Test
    void cambiarClave_conUnaNuevaClaveIgualOCorta_esInvalido() {
        Usuario u = new Usuario("ana@kala.test", "hash-viejo", "Ana");
        when(usuarios.porId(7L)).thenReturn(Optional.of(u));
        when(hasher.coincide(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.cambiarClave(7L, "la-clave-actual-1", "la-clave-actual-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.cambiarClave(7L, "la-clave-actual-1", "corta"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(u.getPasswordHash()).isEqualTo("hash-viejo");
    }

    @Test
    void cambiarClave_deUnUsuarioQueYaNoExiste_es401() {
        when(usuarios.porId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarClave(7L, "la-clave-actual-1", "otra-clave-nueva-2"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    // ---- desactivar y reactivar ----

    private Usuario usuarioConId(long id, boolean activo) {
        Usuario u = new Usuario("u" + id + "@kala.test", "hash", "Persona " + id);
        org.springframework.test.util.ReflectionTestUtils.setField(u, "id", id);
        u.setActivo(activo);
        return u;
    }

    @Test
    void elAdminDesactivaAOtroUsuario() {
        Usuario objetivo = usuarioConId(8L, true);
        when(usuarios.porId(8L)).thenReturn(Optional.of(objetivo));

        service.cambiarEstado(1L, true, null, 8L, false);

        assertThat(objetivo.isActivo()).isFalse();
        verify(usuarios).guardar(objetivo);
    }

    @Test
    void nadieDesactivaSuPropiaCuenta() {
        when(usuarios.porId(1L)).thenReturn(Optional.of(usuarioConId(1L, true)));

        assertThatThrownBy(() -> service.cambiarEstado(1L, true, null, 1L, false))
                .isInstanceOf(IllegalArgumentException.class);
        verify(usuarios, never()).guardar(any());
    }

    @Test
    void elPersonalDesactivaUnEmpleadoDeSuEmpresa() {
        Usuario objetivo = usuarioConId(8L, true);
        when(usuarios.porId(8L)).thenReturn(Optional.of(objetivo));
        when(empleados.porUsuario(8L)).thenReturn(Optional.of(new EmpleadoEquipo(8L, 9L)));
        when(equipos.porId(9L)).thenReturn(Optional.of(new Equipo("Ventas", 4L)));

        service.cambiarEstado(2L, false, 4L, 8L, false);

        assertThat(objetivo.isActivo()).isFalse();
    }

    @Test
    void elPersonalNoToca_empleadosDeOtraEmpresa_niAdministradoresNiPersonal() {
        when(usuarios.porId(8L)).thenReturn(Optional.of(usuarioConId(8L, true)));
        when(empleados.porUsuario(8L)).thenReturn(Optional.of(new EmpleadoEquipo(8L, 9L)));
        when(equipos.porId(9L)).thenReturn(Optional.of(new Equipo("Ventas", 77L)));
        when(usuarios.porId(5L)).thenReturn(Optional.of(usuarioConId(5L, true)));
        when(empleados.porUsuario(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstado(2L, false, 4L, 8L, false))
                .isInstanceOf(UsuarioNoEncontradoException.class);
        assertThatThrownBy(() -> service.cambiarEstado(2L, false, 4L, 5L, false))
                .isInstanceOf(UsuarioNoEncontradoException.class);
        verify(usuarios, never()).guardar(any());
    }

    @Test
    void unUsuarioInexistenteEs404() {
        when(usuarios.porId(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstado(1L, true, null, 8L, false))
                .isInstanceOf(UsuarioNoEncontradoException.class);
    }

    @Test
    void reactivar_dejaActivaLaCuenta_yRepetirLaOperacionNoEscribeNada() {
        Usuario objetivo = usuarioConId(8L, false);
        when(usuarios.porId(8L)).thenReturn(Optional.of(objetivo));

        service.cambiarEstado(1L, true, null, 8L, true);
        service.cambiarEstado(1L, true, null, 8L, true);

        assertThat(objetivo.isActivo()).isTrue();
        verify(usuarios, org.mockito.Mockito.times(1)).guardar(objetivo);
    }

    @Test
    void unaCuentaDesactivadaNoPuedeCambiarSuClave() {
        Usuario inactivo = usuarioConId(7L, false);
        when(usuarios.porId(7L)).thenReturn(Optional.of(inactivo));
        when(hasher.coincide(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.cambiarClave(7L, "la-clave-actual-1", "otra-clave-nueva-2"))
                .isInstanceOf(CredencialesInvalidasException.class);
        verify(usuarios, never()).guardar(any());
    }

    @SuppressWarnings("unused")
    private Empresa empresaDePrueba() {
        return new Empresa("20123456789", "ACME", "TI");
    }
}
