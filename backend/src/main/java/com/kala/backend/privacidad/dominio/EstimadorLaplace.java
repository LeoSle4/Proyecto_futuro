package com.kala.backend.privacidad.dominio;

// Clase pura: sin Spring y sin estado. Estima la media de una escala a partir de valores perturbados con Laplace.
// El ruido tiene media 0, así que la media de los valores reportados es un estimador insesgado: mu = suma / total.
public class EstimadorLaplace {

    private static final double Z_95 = 1.96;

    private final double valorMin;
    private final double valorMax;
    private final double epsilon;

    public EstimadorLaplace(double valorMin, double valorMax, double epsilon) {
        if (!Double.isFinite(valorMin) || !Double.isFinite(valorMax) || valorMax <= valorMin) {
            throw new IllegalArgumentException("valorMax debe ser mayor que valorMin");
        }
        if (!Double.isFinite(epsilon) || epsilon <= 0) {
            throw new IllegalArgumentException("epsilon debe ser mayor que 0");
        }
        this.valorMin = valorMin;
        this.valorMax = valorMax;
        this.epsilon = epsilon;
    }

    public double estimarMedia(double suma, long total) {
        exigirTotalPositivo(total);
        // La media real está dentro de la escala; solo el resultado final se recorta, nunca cada valor ruidoso.
        return Math.max(valorMin, Math.min(valorMax, suma / total));
    }

    // 1.96 * sqrt(sigma^2 + 2 (delta/epsilon)^2) / sqrt(n). El servidor no conoce sigma^2 (la varianza de los
    // valores reales), así que usa su cota máxima para una variable en [min, max]: delta^2 / 4 (Popoviciu).
    // El intervalo resultante es conservador: cubre al menos el 95 %.
    public double margenError95(long total) {
        exigirTotalPositivo(total);
        double delta = valorMax - valorMin;
        double varianzaMaxima = delta * delta / 4.0;
        double varianzaRuido = 2.0 * (delta / epsilon) * (delta / epsilon);
        return Z_95 * Math.sqrt(varianzaMaxima + varianzaRuido) / Math.sqrt(total);
    }

    private static void exigirTotalPositivo(long total) {
        if (total <= 0) {
            throw new IllegalStateException("El total de respuestas debe ser positivo");
        }
    }
}
