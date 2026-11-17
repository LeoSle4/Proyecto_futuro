package com.kala.backend.privacidad.dominio;

// Resultado de estimar un ítem. Randomized Response llena proporcionEstimada (0 a 1); Laplace llena mediaEstimada
// (en la escala del ítem). El margen de error al 95 % está en las mismas unidades que el estimado.
public record Estimacion(Double proporcionEstimada, Double mediaEstimada, double margenError95) {
}
