package com.kala.backend.instrumentos.infraestructura.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

// DTOs del presupuesto de privacidad (ε máximo por persona al mes de una empresa).
public final class PresupuestoDtos {

    private PresupuestoDtos() {
    }

    public record PresupuestoRequest(
            @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("999.999") BigDecimal epsilonMaximoPersona
    ) {
    }

    // epsilonMensual es lo que gasta hoy una persona con los cuestionarios activos de la empresa.
    public record PresupuestoResponse(Long empresaId, String periodo, double epsilonMaximo, double epsilonMensual) {
    }
}
