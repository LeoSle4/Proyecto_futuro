package com.kala.backend.privacidad.dominio;

import java.util.Random;

// Aleatorizador de referencia del dispositivo para Laplace (solo pruebas): valor + Lap(delta / epsilon).
// No recorta el resultado al rango de la escala, porque recortar sesga el estimador.
final class LaplaceReferencia {

    private LaplaceReferencia() {
    }

    static double perturbar(Random azar, double valor, double sensibilidad, double epsilon) {
        double escala = sensibilidad / epsilon;
        double u = azar.nextDouble() - 0.5;
        // Muestreo por la inversa de la CDF; el limite evita log(0).
        double acotado = Math.max(-0.4999999999, Math.min(0.4999999999, u));
        return valor - escala * Math.signum(acotado) * Math.log(1.0 - 2.0 * Math.abs(acotado));
    }
}
