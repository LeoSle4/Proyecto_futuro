package com.kala.backend.privacidad.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class EstimadorLaplaceTest {

    private static final double TOLERANCIA = 0.0001;

    @Test
    void estimaLaMediaComoSumaSobreTotal() {
        EstimadorLaplace estimador = new EstimadorLaplace(0, 3, 1.0);

        assertEquals(1.5, estimador.estimarMedia(150.0, 100), TOLERANCIA);
    }

    @Test
    void recortaLaMediaAlRangoDeLaEscala() {
        EstimadorLaplace estimador = new EstimadorLaplace(0, 3, 1.0);

        assertEquals(0.0, estimador.estimarMedia(-40.0, 100), TOLERANCIA);
        assertEquals(3.0, estimador.estimarMedia(450.0, 100), TOLERANCIA);
    }

    @Test
    void margenDeErrorUsaLaVarianzaMaximaDeUnaVariableAcotada() {
        // delta = 3, epsilon = 1, n = 100: 1.96 * sqrt(9/4 + 2 * 9) / 10 = 0.882
        EstimadorLaplace estimador = new EstimadorLaplace(0, 3, 1.0);

        assertEquals(0.882, estimador.margenError95(100), TOLERANCIA);
    }

    @Test
    void masEpsilonDaMenosMargenYMasRespuestasTambien() {
        EstimadorLaplace estricto = new EstimadorLaplace(0, 5, 0.5);
        EstimadorLaplace laxo = new EstimadorLaplace(0, 5, 2.0);

        assertTrue(laxo.margenError95(50) < estricto.margenError95(50));
        assertTrue(estricto.margenError95(200) < estricto.margenError95(50));
    }

    @Test
    void sinRespuestasNoSePuedeEstimar() {
        EstimadorLaplace estimador = new EstimadorLaplace(0, 3, 1.0);

        assertThrows(IllegalStateException.class, () -> estimador.estimarMedia(0.0, 0));
        assertThrows(IllegalStateException.class, () -> estimador.margenError95(0));
    }

    @Test
    void rechazaParametrosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new EstimadorLaplace(3, 3, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new EstimadorLaplace(0, 3, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new EstimadorLaplace(0, 3, Double.NaN));
    }

    // Simulacion Monte Carlo con el aleatorizador de referencia: la media es insesgada y el intervalo al 95 %
    // contiene la media real al menos el 95 % de las veces (la cota de varianza es conservadora).
    @Test
    void monteCarlo_esInsesgadoYElIntervaloCubreLaMediaReal() {
        double epsilon = 1.0;
        int n = 400;
        int repeticiones = 600;
        Random azar = new Random(20261117L);
        EstimadorLaplace estimador = new EstimadorLaplace(0, 3, epsilon);

        double sumaEstimaciones = 0;
        int cubiertas = 0;
        double mediaReal = 0;
        for (int r = 0; r < repeticiones; r++) {
            double suma = 0;
            double sumaReal = 0;
            for (int i = 0; i < n; i++) {
                int valor = azar.nextInt(4);
                sumaReal += valor;
                suma += LaplaceReferencia.perturbar(azar, valor, 3, epsilon);
            }
            mediaReal = sumaReal / n;
            double media = suma / n; // sin recortar: el recorte sesga el estimador
            sumaEstimaciones += media - mediaReal;
            if (Math.abs(media - mediaReal) <= estimador.margenError95(n)) {
                cubiertas++;
            }
        }

        assertEquals(0.0, sumaEstimaciones / repeticiones, 0.05);
        assertTrue(cubiertas / (double) repeticiones >= 0.95,
                "cobertura observada: " + cubiertas / (double) repeticiones);
    }
}
