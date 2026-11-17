package com.kala.backend.privacidad.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Generador de datos de demostración: simula lo que hacen los dispositivos. La semilla es fija para que la demo se
// pueda repetir, y no se elige por el resultado que produce. La proporción o la media reales las conoce solo el
// generador: sirven para comparar con la estimación y nunca se envían al servidor.
public final class GeneradorDatosDemo {

    public static final long SEMILLA_DEMO = 20261117L;

    // Respuestas ya perturbadas con Randomized Response, listas para enviar.
    public record LoteRandomizedResponse(List<Boolean> ruidosas, double proporcionReal) {
    }

    // Valores ya perturbados con Laplace y la media real de los valores originales.
    public record LoteLaplace(List<Double> ruidosos, double mediaReal) {
    }

    private final Random azar;

    public GeneradorDatosDemo(long semilla) {
        this.azar = new Random(semilla);
    }

    // n personas cuya proporción real de "sí" es piReal; cada una aleatoriza con probabilidad de verdad p.
    public LoteRandomizedResponse randomizedResponse(int n, double piReal, double p) {
        List<Boolean> ruidosas = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            boolean real = azar.nextDouble() < piReal;
            ruidosas.add(RandomizedResponseReferencia.perturbar(real, p, azar));
        }
        return new LoteRandomizedResponse(ruidosas, piReal);
    }

    // n personas con un valor entero en [min, max] (con sesgo hacia valores bajos, como una escala de síntomas);
    // cada una suma ruido Lap((max - min) / epsilon) sin recortar.
    public LoteLaplace laplace(int n, int min, int max, double epsilon) {
        List<Double> ruidosos = new ArrayList<>(n);
        double sumaReal = 0;
        int rango = max - min;
        for (int i = 0; i < n; i++) {
            int real = min + (int) Math.min(rango, Math.round(Math.abs(azar.nextGaussian()) * rango / 3.0));
            sumaReal += real;
            ruidosos.add(LaplaceReferencia.perturbar(azar, real, rango, epsilon));
        }
        return new LoteLaplace(ruidosos, sumaReal / n);
    }
}
