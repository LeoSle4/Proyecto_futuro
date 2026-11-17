package com.kala.backend.privacidad.dominio;

import java.math.BigDecimal;
import java.util.Optional;

// Laplace: el dispositivo manda el valor más ruido Lap(delta / epsilon), sin recortarlo al rango de la escala.
public final class Laplace implements MecanismoLdp {

    public static final String CODIGO = "LAPLACE";

    // El ruido Laplace de escala b = delta / epsilon supera a k b con probabilidad e^-k: con k = 50 es del orden de
    // 2e-22, así que un valor más lejos que eso de la escala del ítem no puede venir de un dispositivo honesto.
    private static final double ESCALAS_DE_RUIDO_ADMITIDAS = 50;

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
    public BigDecimal aSuma(Boolean respuestaRuidosa, BigDecimal valorRuidoso, ParametrosLdp parametros,
                            double valorMin, double valorMax) {
        if (valorRuidoso == null) {
            throw new IllegalArgumentException("Un ítem Laplace requiere valorRuidoso");
        }
        double epsilon = epsilonItem(parametros);
        double margen = ESCALAS_DE_RUIDO_ADMITIDAS * (valorMax - valorMin) / epsilon;
        double valor = valorRuidoso.doubleValue();
        if (!Double.isFinite(valor) || valor < valorMin - margen || valor > valorMax + margen) {
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
