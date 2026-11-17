package com.kala.backend.ayuda.dominio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 404: el recurso no existe o es de otra empresa (no se distingue, para no revelar su existencia).
@ResponseStatus(HttpStatus.NOT_FOUND)
public class SolicitudAyudaNoEncontradaException extends RuntimeException {

    public SolicitudAyudaNoEncontradaException(Long id) {
        super("La solicitud de ayuda no existe en la empresa: " + id);
    }
}
