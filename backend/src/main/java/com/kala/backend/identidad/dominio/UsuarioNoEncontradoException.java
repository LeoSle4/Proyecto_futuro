package com.kala.backend.identidad.dominio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 404: el usuario no existe o no es de la empresa de quien consulta (no se distingue, para no revelar su existencia).
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException(Long id) {
        super("No existe el usuario con id: " + id);
    }
}
