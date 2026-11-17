package com.kala.backend.privacidad.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeneradorDatosDemoTest {

    @Test
    void conLaMismaSemillaLaDemoSeRepite_yConOtraCambia() {
        var a = new GeneradorDatosDemo(GeneradorDatosDemo.SEMILLA_DEMO).randomizedResponse(30, 0.3, 0.75);
        var b = new GeneradorDatosDemo(GeneradorDatosDemo.SEMILLA_DEMO).randomizedResponse(30, 0.3, 0.75);
        var c = new GeneradorDatosDemo(GeneradorDatosDemo.SEMILLA_DEMO + 1).randomizedResponse(30, 0.3, 0.75);

        assertThat(a.ruidosas()).isEqualTo(b.ruidosas());
        assertThat(a.ruidosas()).isNotEqualTo(c.ruidosas());
    }

    @Test
    void randomizedResponse_lasRespuestasQuedanPerturbadasYElEstimadorRecuperaLaProporcion() {
        var lote = new GeneradorDatosDemo(GeneradorDatosDemo.SEMILLA_DEMO).randomizedResponse(20_000, 0.3, 0.75);
        int si = (int) lote.ruidosas().stream().filter(r -> r).count();

        // λ esperado = 0.3 × 0.75 + 0.7 × 0.25 = 0.40, y no 0.30: el dato enviado no es el real.
        assertThat(si / 20_000.0).isBetween(0.38, 0.42);
        assertThat(new EstimadorDesesgado(0.75).estimarProporcion(si, 20_000)).isBetween(0.27, 0.33);
    }

    @Test
    void laplace_losValoresNoSeRecortanYLaMediaSeRecupera() {
        var lote = new GeneradorDatosDemo(GeneradorDatosDemo.SEMILLA_DEMO).laplace(20_000, 0, 5, 1.0);
        double media = lote.ruidosos().stream().mapToDouble(Double::doubleValue).average().orElseThrow();

        assertThat(lote.ruidosos()).anyMatch(v -> v < 0 || v > 5);
        assertThat(media).isCloseTo(lote.mediaReal(), org.assertj.core.data.Offset.offset(0.15));
    }
}
