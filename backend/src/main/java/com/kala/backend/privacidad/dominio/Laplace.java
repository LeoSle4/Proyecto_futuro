package com.kala.backend.privacidad.dominio;

import java.math.BigDecimal;
import java.util.Optional;

// Laplace: el dispositivo manda el valor más ruido Lap(delta / epsilon), sin recortarlo al rango de la escala.
public final class Laplace implements MecanismoLdp {

    public static final String CODIGO = "LAPLACE";

    // Cota de cordura para un valor ruidoso: con Laplace la cola es larga, pero nunca llega a este orden.
    private static final BigDecimal LIMITE_VALOR = new BigDecimal("1000000");

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public double epsilonItem(ParametrosLdp parametros) {
        if (parametros.epsilon() == null || parametros.epsilon().signum() <= 0) {
            throw new IllegalArgumentException("Laplace requiere un epsilon positivo");
        }
        return parametros.epsilon().doubleValue();
    }

    @Override
    public BigDecimal aSuma(Boolean respuestaRuidosa, BigDecimal valorRuidoso) {
        if (valorRuidoso == null) {
            throw new IllegalArgumentException("Un ítem Laplace requiere valorRuidoso");
        }
        if (valorRuidoso.abs().compareTo(LIMITE_VALOR) > 0) {
            throw new IllegalArgumentException("valorRuidoso está fuera del rango admitido");
        }
        return valorRuidoso;
    }

    @Override
    public Optional<Estimacion> estimar(ParametrosLdp parametros, double valorMin, double valorMax,
                                        BigDecimal suma, long total) {
        if (parametros.epsilon() == null) {
            return Optional.empty();
        }
        EstimadorLaplace estimador = new EstimadorLaplace(valorMin, valorMax, parametros.epsilon().doubleValue());
        return Optional.of(new Estimacion(null, estimador.estimarMedia(suma.doubleValue(), total),
                estimador.margenError95(total)));
    }
}
