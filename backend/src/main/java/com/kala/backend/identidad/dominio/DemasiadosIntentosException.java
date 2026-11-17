package com.kala.backend.identidad.dominio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 429: demasiados inicios de sesión fallidos para ese correo. El mensaje no dice si el correo existe.
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class DemasiadosIntentosException extends RuntimeException {

    private final long reintentarEnSegundos;

    public DemasiadosIntentosException(long reintentarEnSegundos) {
        super("Demasiados intentos fallidos; intenta de nuevo más tarde");
        this.reintentarEnSegundos = reintentarEnSegundos;
    }

    public long getReintentarEnSegundos() {
        return reintentarEnSegundos;
    }
}
