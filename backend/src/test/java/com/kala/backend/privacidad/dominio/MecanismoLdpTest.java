package com.kala.backend.privacidad.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

class MecanismoLdpTest {

    private static final Offset<Double> TOL = Offset.offset(1e-9);
    private static final ParametrosLdp RR = new ParametrosLdp(new BigDecimal("0.750"), null);
    private static final ParametrosLdp LAP = new ParametrosLdp(null, new BigDecimal("1.000"));

    // ---- registro ----

    @Test
    void elRegistroEncuentraCadaMecanismoPorSuCodigo() {
        assertThat(MecanismosLdp.de("RANDOMIZED_RESPONSE")).isInstanceOf(RandomizedResponse.class);
        assertThat(MecanismosLdp.de("LAPLACE")).isInstanceOf(Laplace.class);
        assertThat(MecanismosLdp.buscar("INVENTADO")).isEmpty();
    }

    @Test
    void unMecanismoDesconocidoEsInvalido() {
        assertThatThrownBy(() -> MecanismosLdp.de("INVENTADO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mecanismo desconocido: INVENTADO");
    }

    // ---- Randomized Response ----

    @Test
    void rr_epsilonDelItemEsLnDeP() {
        assertThat(MecanismosLdp.de("RANDOMIZED_RESPONSE").epsilonItem(RR)).isEqualTo(Math.log(3.0), TOL);
    }

    @Test
    void rr_rechazaParametrosInvalidos() {
        var rr = MecanismosLdp.de("RANDOMIZED_RESPONSE");

        assertThatThrownBy(() -> rr.epsilonItem(new ParametrosLdp(null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Randomized Response requiere probabilidadVerdad");
        assertThatThrownBy(() -> rr.epsilonItem(new ParametrosLdp(new BigDecimal("0.5"), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rr.epsilonItem(new ParametrosLdp(BigDecimal.ONE, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rr_laRespuestaSeSumaComoUnoOCero_yExigeRespuestaRuidosa() {
        var rr = MecanismosLdp.de("RANDOMIZED_RESPONSE");

        assertThat(rr.aSuma(true, null, RR, 0, 1)).isEqualByComparingTo("1");
        assertThat(rr.aSuma(false, new BigDecimal("9"), RR, 0, 1)).isEqualByComparingTo("0");
        assertThatThrownBy(() -> rr.aSuma(null, new BigDecimal("1"), RR, 0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Un ítem Randomized Response requiere respuestaRuidosa");
    }

    @Test
    void rr_estimaLaProporcion() {
        // λ = 21 / 30 = 0.70 -> π = (0.70 − 0.25) / 0.5 = 0.90
        var e = MecanismosLdp.de("RANDOMIZED_RESPONSE").estimar(RR, 0, 1, new BigDecimal("21"), 30).orElseThrow();

        assertThat(e.proporcionEstimada()).isEqualTo(0.90, TOL);
        assertThat(e.mediaEstimada()).isNull();
        assertThat(e.margenError95()).isEqualTo(new EstimadorDesesgado(0.75).margenError95(21, 30), TOL);
    }

    @Test
    void rr_sinProbabilidadNoSePuedeEstimar() {
        assertThat(MecanismosLdp.de("RANDOMIZED_RESPONSE")
                .estimar(new ParametrosLdp(null, null), 0, 1, BigDecimal.TEN, 30)).isEmpty();
    }

    // ---- Laplace ----

    @Test
    void laplace_epsilonDelItemEsElConfigurado() {
        assertThat(MecanismosLdp.de("LAPLACE").epsilonItem(LAP)).isEqualTo(1.0, TOL);
    }

    @Test
    void laplace_rechazaEpsilonAusenteONoPositivo() {
        var laplace = MecanismosLdp.de("LAPLACE");

        assertThatThrownBy(() -> laplace.epsilonItem(new ParametrosLdp(null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Laplace requiere un epsilon positivo");
        assertThatThrownBy(() -> laplace.epsilonItem(new ParametrosLdp(null, BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laplace_sumaElValorRuidosoSinRecortar_yExigeValorRuidoso() {
        var laplace = MecanismosLdp.de("LAPLACE");

        assertThat(laplace.aSuma(null, new BigDecimal("-1.37"), LAP, 0, 5)).isEqualByComparingTo("-1.37");
        assertThatThrownBy(() -> laplace.aSuma(true, null, LAP, 0, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Un ítem Laplace requiere valorRuidoso");
    }

    // La cota depende del ítem: la escala (delta = max - min) y epsilon fijan la escala del ruido b = delta / epsilon;
    // se admite hasta 50 b fuera de la escala (la probabilidad de pasarse es del orden de 2e-22).
    @Test
    void laplace_aceptaHastaCincuentaEscalasDeRuidoFueraDeLaEscalaDelItem() {
        var laplace = MecanismosLdp.de("LAPLACE");

        // delta = 5, epsilon = 1 -> b = 5 -> el valor puede ir de 0 - 250 a 5 + 250.
        assertThat(laplace.aSuma(null, new BigDecimal("255"), LAP, 0, 5)).isEqualByComparingTo("255");
        assertThat(laplace.aSuma(null, new BigDecimal("-250"), LAP, 0, 5)).isEqualByComparingTo("-250");
        assertThatThrownBy(() -> laplace.aSuma(null, new BigDecimal("255.01"), LAP, 0, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("valorRuidoso está fuera del rango admitido");
        assertThatThrownBy(() -> laplace.aSuma(null, new BigDecimal("-250.01"), LAP, 0, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("valorRuidoso está fuera del rango admitido");
    }

    @Test
    void laplace_conMasEpsilonLaCotaSeEstrecha_yConUnaEscalaMasCortaTambien() {
        var laplace = MecanismosLdp.de("LAPLACE");
        var epsilonDiez = new ParametrosLdp(null, new BigDecimal("10.000"));

        // delta = 3, epsilon = 10 -> b = 0.3 -> hasta 3 + 15 = 18.
        assertThat(laplace.aSuma(null, new BigDecimal("18"), epsilonDiez, 0, 3)).isEqualByComparingTo("18");
        assertThatThrownBy(() -> laplace.aSuma(null, new BigDecimal("18.5"), epsilonDiez, 0, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laplace_sinEpsilonEnLaConfiguracion_noPuedeValidarElValor() {
        var laplace = MecanismosLdp.de("LAPLACE");

        assertThatThrownBy(() -> laplace.aSuma(null, BigDecimal.ONE, new ParametrosLdp(null, null), 0, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Laplace requiere un epsilon positivo");
    }

    @Test
    void laplace_estimaLaMediaConSuMargen() {
        var e = MecanismosLdp.de("LAPLACE").estimar(LAP, 0, 5, new BigDecimal("90"), 30).orElseThrow();

        assertThat(e.mediaEstimada()).isEqualTo(3.0, TOL);
        assertThat(e.proporcionEstimada()).isNull();
        assertThat(e.margenError95()).isEqualTo(new EstimadorLaplace(0, 5, 1.0).margenError95(30), TOL);
    }

    @Test
    void laplace_sinEpsilonNoSePuedeEstimar() {
        assertThat(MecanismosLdp.de("LAPLACE")
                .estimar(new ParametrosLdp(null, null), 0, 5, BigDecimal.TEN, 30)).isEmpty();
    }
}
