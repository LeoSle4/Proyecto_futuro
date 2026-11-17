package com.kala.backend.ayuda.dominio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 409: la solicitud no está en un estado que permita la operación (por ejemplo, atender una ya cerrada).
@ResponseStatus(HttpStatus.CONFLICT)
public class SolicitudAyudaEstadoException extends RuntimeException {

    public SolicitudAyudaEstadoException(Long id, String estado) {
        super("La solicitud " + id + " está " + estado + " y no admite esta operación");
    }
}
