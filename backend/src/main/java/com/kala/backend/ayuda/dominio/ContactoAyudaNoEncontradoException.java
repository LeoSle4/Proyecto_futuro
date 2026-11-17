package com.kala.backend.ayuda.dominio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 404: el recurso no existe o es de otra empresa (no se distingue, para no revelar su existencia).
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ContactoAyudaNoEncontradoException extends RuntimeException {

    public ContactoAyudaNoEncontradoException(Long id) {
        super("El contacto de ayuda no existe en la empresa: " + id);
    }
}
