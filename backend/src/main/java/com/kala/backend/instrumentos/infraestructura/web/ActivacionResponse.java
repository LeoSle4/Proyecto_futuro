package com.kala.backend.instrumentos.infraestructura.web;

// Resultado de activar un cuestionario: el ε mensual por persona resultante y el máximo vigente.
public record ActivacionResponse(
        Long empresaId,
        Long cuestionarioId,
        double epsilonMensual,
        double epsilonMaximo
) {
}
