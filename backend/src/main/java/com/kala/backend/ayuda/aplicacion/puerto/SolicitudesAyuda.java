package com.kala.backend.ayuda.aplicacion.puerto;

import com.kala.backend.ayuda.dominio.SolicitudAyuda;
import java.util.List;
import java.util.Optional;

// Puerto de salida: las solicitudes de ayuda, el único canal identificado de Kala.
public interface SolicitudesAyuda {

    SolicitudAyuda guardar(SolicitudAyuda solicitud);

    List<SolicitudAyuda> deUsuario(Long usuarioId);

    // La empresa de una solicitud se deriva de la persona (empleado -> equipo -> empresa).
    List<SolicitudAyuda> deEmpresa(Long empresaId);

    Optional<SolicitudAyuda> buscarDeEmpresa(Long id, Long empresaId);
}
