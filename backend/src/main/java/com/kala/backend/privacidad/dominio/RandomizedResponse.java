package com.kala.backend.privacidad.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

// Randomized Response: el dispositivo manda la respuesta real con probabilidad p y la contraria con 1 - p.
public final class RandomizedResponse implements MecanismoLdp {

    public static final String CODIGO = "RANDOMIZED_RESPONSE";

    @Override
    public String codigo() {
        return CODIGO;
    }

    // ε = ln(p / (1 - p)).
    @Override
    public double epsilonItem(ParametrosLdp parametros) {
        if (parametros.probabilidadVerdad() == null) {
            throw new IllegalArgumentException("Randomized Response requiere probabilidadVerdad");
        }
        double p = parametros.probabilidadVerdad().doubleValue();
        if (p <= 0.5 || p >= 1.0) {
            throw new IllegalArgumentException("p debe ser mayor que 0.5 y menor que 1.0");
        }
        return Math.log(p / (1.0 - p));
    }

    @Override
    public BigDecimal aSuma(Boolean respuestaRuidosa, BigDecimal valorRuidoso, ParametrosLdp parametros,
                            double valorMin, double valorMax) {
        if (respuestaRuidosa == null) {
            throw new IllegalArgumentException("Un ítem Randomized Response requiere respuestaRuidosa");
        }
        return respuestaRuidosa ? BigDecimal.ONE : BigDecimal.ZERO;
    }

    @Override
    public Optional<Estimacion> estimar(ParametrosLdp parametros, double valorMin, double valorMax,
                                        BigDecimal suma, long total) {
        if (parametros.probabilidadVerdad() == null) {
            return Optional.empty();
        }
        int si = suma.setScale(0, RoundingMode.HALF_UP).intValueExact();
        int n = Math.toIntExact(total);
        EstimadorDesesgado estimador = new EstimadorDesesgado(parametros.probabilidadVerdad().doubleValue());
        return Optional.of(new Estimacion(estimador.estimarProporcion(si, n), null, estimador.margenError95(si, n)));
    }
}
