package com.kala.backend.identidad.dominio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 409: ya existe una cuenta con ese correo. El mensaje no incluye el correo recibido.
@ResponseStatus(HttpStatus.CONFLICT)
public class EmailEnUsoException extends RuntimeException {

    public EmailEnUsoException() {
        super("Ya existe una cuenta con ese correo");
    }
}
