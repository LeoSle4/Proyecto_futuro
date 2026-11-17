package com.kala.backend.checkin.dominio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 400: el envío no corresponde al ítem o al cuestionario. El mensaje nunca incluye el valor recibido, porque es
// la respuesta ruidosa y no debe aparecer en una respuesta de error ni en un log.
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CheckInInvalidoException extends IllegalArgumentException {

    public CheckInInvalidoException(String motivo) {
        super(motivo);
    }
}
