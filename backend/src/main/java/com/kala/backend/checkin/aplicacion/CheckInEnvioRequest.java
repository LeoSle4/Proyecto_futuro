package com.kala.backend.checkin.aplicacion;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

// Envío de un ítem desde el dispositivo. La respuesta llega YA perturbada (privacidad diferencial local).
// Randomized Response manda respuestaRuidosa (verdadero o falso); Laplace manda valorRuidoso (número real, sin
// recortar al rango de la escala). El servicio exige el campo que corresponde al mecanismo del ítem.
// El equipo y la persona no vienen en el cuerpo: salen del token.
public record CheckInEnvioRequest(
        @NotNull Long itemId,
        @NotNull Long configuracionLdpId,
        Boolean respuestaRuidosa,
        BigDecimal valorRuidoso
) {

    public CheckInEnvioRequest(Long itemId, Long configuracionLdpId, Boolean respuestaRuidosa) {
        this(itemId, configuracionLdpId, respuestaRuidosa, null);
    }

    // Privacidad: los frameworks imprimen toString() al registrar argumentos (Spring MVC y Hibernate Validator lo
    // hacen en nivel TRACE). Se redacta para que el cuerpo del check-in no aparezca nunca en un log.
    @Override
    public String toString() {
        return "CheckInEnvioRequest[redactado]";
    }
}
