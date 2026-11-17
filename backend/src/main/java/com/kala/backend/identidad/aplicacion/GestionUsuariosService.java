package com.kala.backend.identidad.aplicacion;

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
import com.kala.backend.organizacion.dominio.EmpresaNoEncontradaException;
import com.kala.backend.organizacion.dominio.EquipoNoEncontradoException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Alta de cuentas y cambio de la propia clave. El ADMIN crea personal de empresa; el personal crea empleados, y solo
// en equipos de su empresa (la empresa llega del token, nunca del cuerpo). Ningún mensaje de error repite una clave.
@Service
public class GestionUsuariosService {

    static final int CLAVE_MINIMA = 12;
    // BCrypt trabaja con los primeros 72 bytes: más largo se descartaría en silencio.
    static final int CLAVE_MAXIMA_BYTES = 72;
    private static final int LARGO_MAXIMO = 150;
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final Usuarios usuarios;
    private final Empleados empleados;
    private final PersonalDeEmpresa personal;
    private final HasherDeClaves hasher;
    private final Empresas empresas;
    private final Equipos equipos;

    public GestionUsuariosService(Usuarios usuarios, Empleados empleados, PersonalDeEmpresa personal,
                                  HasherDeClaves hasher, Empresas empresas, Equipos equipos) {
        this.usuarios = usuarios;
        this.empleados = empleados;
        this.personal = personal;
        this.hasher = hasher;
        this.empresas = empresas;
        this.equipos = equipos;
    }

    @Transactional
    public Usuario crearPersonal(Long empresaId, String email, String nombre, String clave, RolPersonal rol) {
        String correo = normalizarCorreo(email);
        String nombreLimpio = validarNombre(nombre);
        validarClave(clave);
        if (rol == null) {
            throw new IllegalArgumentException("El rol es obligatorio");
        }
        if (!empresas.existe(empresaId)) {
            throw new EmpresaNoEncontradaException(empresaId);
        }
        Usuario usuario = nuevoUsuario(correo, nombreLimpio, clave);
        personal.guardar(new PersonalEmpresa(usuario.getId(), empresaId, rol));
        return usuario;
    }

    @Transactional
    public Usuario crearEmpleado(Long empresaId, Long equipoId, String email, String nombre, String clave) {
        String correo = normalizarCorreo(email);
        String nombreLimpio = validarNombre(nombre);
        validarClave(clave);
        // Un equipo de otra empresa responde 404 como si no existiera.
        equipos.porIdEnEmpresa(equipoId, empresaId).orElseThrow(() -> new EquipoNoEncontradoException(equipoId));
        Usuario usuario = nuevoUsuario(correo, nombreLimpio, clave);
        empleados.guardar(new EmpleadoEquipo(usuario.getId(), equipoId));
        return usuario;
    }

    @Transactional
    public void cambiarClave(Long usuarioId, String claveActual, String claveNueva) {
        Usuario usuario = usuarios.porId(usuarioId).orElseThrow(CredencialesInvalidasException::new);
        if (!usuario.isActivo()) {
            throw new CredencialesInvalidasException();
        }
        if (claveActual == null || !hasher.coincide(claveActual, usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }
        validarClave(claveNueva);
        if (claveNueva.equals(claveActual)) {
            throw new IllegalArgumentException("La clave nueva debe ser distinta de la actual");
        }
        usuario.cambiarClave(hasher.cifrar(claveNueva));
        usuarios.guardar(usuario);
    }

    // Activa o desactiva una cuenta. El ADMIN puede con cualquiera menos la suya; el personal de empresa solo con
    // empleados de equipos de su empresa (cualquier otro objetivo responde 404). Es idempotente: si ya está en ese
    // estado no escribe. Una cuenta desactivada no inicia sesión; los tokens ya emitidos valen hasta que venzan.
    @Transactional
    public void cambiarEstado(Long actorId, boolean actorEsAdmin, Long actorEmpresaId, Long usuarioId, boolean activo) {
        Usuario usuario = usuarios.porId(usuarioId).orElseThrow(() -> new UsuarioNoEncontradoException(usuarioId));
        if (!actorEsAdmin && !esEmpleadoDeLaEmpresa(usuarioId, actorEmpresaId)) {
            throw new UsuarioNoEncontradoException(usuarioId);
        }
        if (usuarioId.equals(actorId)) {
            throw new IllegalArgumentException("No puedes cambiar el estado de tu propia cuenta");
        }
        if (usuario.isActivo() != activo) {
            usuario.setActivo(activo);
            usuarios.guardar(usuario);
        }
    }

    private boolean esEmpleadoDeLaEmpresa(Long usuarioId, Long empresaId) {
        return empleados.porUsuario(usuarioId)
                .flatMap(e -> equipos.porId(e.getEquipoId()))
                .map(equipo -> equipo.getEmpresaId().equals(empresaId))
                .orElse(false);
    }

    private Usuario nuevoUsuario(String correo, String nombre, String clave) {
        if (usuarios.existeEmail(correo)) {
            throw new EmailEnUsoException();
        }
        return usuarios.guardar(new Usuario(correo, hasher.cifrar(clave), nombre));
    }

    private static String normalizarCorreo(String email) {
        String correo = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
        if (correo.length() > LARGO_MAXIMO || !CORREO.matcher(correo).matches()) {
            throw new IllegalArgumentException("El correo no es válido");
        }
        return correo;
    }

    private static String validarNombre(String nombre) {
        String limpio = nombre == null ? "" : nombre.strip();
        if (limpio.isEmpty() || limpio.length() > LARGO_MAXIMO) {
            throw new IllegalArgumentException("El nombre es obligatorio y no puede superar " + LARGO_MAXIMO + " caracteres");
        }
        return limpio;
    }

    private static void validarClave(String clave) {
        if (clave == null || clave.length() < CLAVE_MINIMA
                || clave.getBytes(StandardCharsets.UTF_8).length > CLAVE_MAXIMA_BYTES) {
            throw new IllegalArgumentException(
                    "La clave debe tener entre " + CLAVE_MINIMA + " y " + CLAVE_MAXIMA_BYTES + " caracteres");
        }
    }
}
