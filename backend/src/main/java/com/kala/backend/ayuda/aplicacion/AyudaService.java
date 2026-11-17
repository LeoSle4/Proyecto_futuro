package com.kala.backend.ayuda.aplicacion;

import com.kala.backend.ayuda.aplicacion.puerto.ContactosAyuda;
import com.kala.backend.ayuda.aplicacion.puerto.SolicitudesAyuda;
import com.kala.backend.ayuda.dominio.ContactoAyuda;
import com.kala.backend.ayuda.dominio.ContactoAyudaNoEncontradoException;
import com.kala.backend.ayuda.dominio.SolicitudAyuda;
import com.kala.backend.ayuda.dominio.SolicitudAyudaEstadoException;
import com.kala.backend.ayuda.dominio.SolicitudAyudaNoEncontradaException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Canal "Hablar con alguien": contactos curados por la empresa y solicitudes identificadas de los empleados.
// Nada de aquí toca el check-in (R8). El aislamiento por empresa vive en las consultas del repositorio.
@Service
public class AyudaService {

    private final ContactosAyuda contactos;
    private final SolicitudesAyuda solicitudes;
    private final Clock reloj;

    public AyudaService(ContactosAyuda contactos, SolicitudesAyuda solicitudes, Clock reloj) {
        this.contactos = contactos;
        this.solicitudes = solicitudes;
        this.reloj = reloj;
    }

    public List<ContactoAyuda> listarContactos(Long empresaId, boolean soloActivos) {
        return soloActivos
                ? contactos.activosDeEmpresa(empresaId)
                : contactos.deEmpresa(empresaId);
    }

    @Transactional
    public ContactoAyuda crearContacto(Long empresaId, String nombre, String tipo, String telefono, String email) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del contacto es obligatorio");
        }
        if (tipo == null || !ContactoAyuda.TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("El tipo debe ser uno de " + ContactoAyuda.TIPOS);
        }
        String tel = vacioANulo(telefono);
        String correo = vacioANulo(email);
        if (tel == null && correo == null) {
            throw new IllegalArgumentException("El contacto necesita un teléfono o un email");
        }
        return contactos.guardar(new ContactoAyuda(empresaId, nombre.trim(), tipo, tel, correo));
    }

    @Transactional
    public void desactivarContacto(Long empresaId, Long contactoId) {
        ContactoAyuda contacto = contactos.buscarDeEmpresa(contactoId, empresaId)
                .orElseThrow(() -> new ContactoAyudaNoEncontradoException(contactoId));
        contacto.desactivar();
        contactos.guardar(contacto);
    }

    @Transactional
    public SolicitudAyuda solicitar(Long usuarioId, String mensaje) {
        return solicitudes.guardar(new SolicitudAyuda(usuarioId, vacioANulo(mensaje)));
    }

    public List<SolicitudAyuda> misSolicitudes(Long usuarioId) {
        return solicitudes.deUsuario(usuarioId);
    }

    public List<SolicitudAyuda> solicitudesDeEmpresa(Long empresaId, String estado) {
        List<SolicitudAyuda> todas = solicitudes.deEmpresa(empresaId);
        if (estado == null || estado.isBlank()) {
            return todas;
        }
        return todas.stream().filter(s -> s.getEstado().equals(estado)).toList();
    }

    // PENDIENTE -> EN_ATENCION. El contacto es opcional, pero si viene debe estar activo y ser de la empresa.
    @Transactional
    public SolicitudAyuda atender(Long empresaId, Long personalId, Long solicitudId, Long contactoId) {
        SolicitudAyuda solicitud = solicitudDeEmpresa(empresaId, solicitudId);
        if (!SolicitudAyuda.PENDIENTE.equals(solicitud.getEstado())) {
            throw new SolicitudAyudaEstadoException(solicitudId, solicitud.getEstado());
        }
        if (contactoId != null) {
            // Solo se valida: un contacto inactivo o de otra empresa responde 404 como si no existiera.
            contactos.buscarDeEmpresa(contactoId, empresaId)
                    .filter(ContactoAyuda::isActivo)
                    .orElseThrow(() -> new ContactoAyudaNoEncontradoException(contactoId));
        }
        solicitud.atender(personalId, contactoId, ahora());
        return solicitudes.guardar(solicitud);
    }

    @Transactional
    public SolicitudAyuda cerrar(Long empresaId, Long personalId, Long solicitudId) {
        SolicitudAyuda solicitud = solicitudDeEmpresa(empresaId, solicitudId);
        if (SolicitudAyuda.CERRADA.equals(solicitud.getEstado())) {
            throw new SolicitudAyudaEstadoException(solicitudId, solicitud.getEstado());
        }
        solicitud.cerrar(personalId, ahora());
        return solicitudes.guardar(solicitud);
    }

    private SolicitudAyuda solicitudDeEmpresa(Long empresaId, Long solicitudId) {
        return solicitudes.buscarDeEmpresa(solicitudId, empresaId)
                .orElseThrow(() -> new SolicitudAyudaNoEncontradaException(solicitudId));
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
