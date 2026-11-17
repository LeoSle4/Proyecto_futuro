package com.kala.backend.privacidad.dominio;

import java.math.BigDecimal;

// Parámetros de una configuración LDP: Randomized Response usa probabilidadVerdad; Laplace usa epsilon.
public record ParametrosLdp(BigDecimal probabilidadVerdad, BigDecimal epsilon) {
}
