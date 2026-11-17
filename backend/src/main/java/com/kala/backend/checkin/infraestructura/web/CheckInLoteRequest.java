package com.kala.backend.checkin.infraestructura.web;

import com.kala.backend.checkin.aplicacion.CheckInEnvioRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

// Envío de una batería: hasta k ítems del mismo cuestionario, que comparten una sola sesión de antiduplicado.
public record CheckInLoteRequest(
        @NotEmpty List<@Valid CheckInEnvioRequest> respuestas
) {

    // Privacidad: igual que CheckInEnvioRequest, el cuerpo nunca debe llegar a un log.
    @Override
    public String toString() {
        return "CheckInLoteRequest[redactado]";
    }
}
