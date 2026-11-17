package com.kala.backend.identidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kala.backend.identidad.dominio.DemasiadosIntentosException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LimitadorIntentosLoginTest {

    // Reloj que se puede avanzar a mano.
    private static final class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-11-17T15:00:00Z");

        void avanzar(Duration d) {
            ahora = ahora.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    private RelojManual reloj;
    private LimitadorIntentosLogin limitador;

    @BeforeEach
    void setUp() {
        reloj = new RelojManual();
        limitador = new LimitadorIntentosLogin(5, Duration.ofMinutes(15), Duration.ofMinutes(15), reloj);
    }

    private void fallar(String clave, int veces) {
        for (int i = 0; i < veces; i++) {
            limitador.registrarFallo(clave);
        }
    }

    @Test
    void conMenosDelMaximoDeFallosSigueDejandoIntentar() {
        fallar("ana@kala.test", 4);

        assertThatCode(() -> limitador.verificar("ana@kala.test")).doesNotThrowAnyException();
    }

    @Test
    void alLlegarAlMaximoDeFallosBloqueaYDiceCuantoEsperar() {
        fallar("ana@kala.test", 5);

        assertThatThrownBy(() -> limitador.verificar("ana@kala.test"))
                .isInstanceOfSatisfying(DemasiadosIntentosException.class,
                        e -> assertThat(e.getReintentarEnSegundos()).isBetween(890L, 900L));
    }

    @Test
    void elBloqueoTerminaConElTiempo() {
        fallar("ana@kala.test", 5);
        reloj.avanzar(Duration.ofMinutes(16));

        assertThatCode(() -> limitador.verificar("ana@kala.test")).doesNotThrowAnyException();
        // Tras el bloqueo se empieza de cero: un fallo suelto no vuelve a bloquear.
        limitador.registrarFallo("ana@kala.test");
        assertThatCode(() -> limitador.verificar("ana@kala.test")).doesNotThrowAnyException();
    }

    @Test
    void unInicioDeSesionExitosoReiniciaLaCuenta() {
        fallar("ana@kala.test", 4);
        limitador.registrarExito("ana@kala.test");
        fallar("ana@kala.test", 4);

        assertThatCode(() -> limitador.verificar("ana@kala.test")).doesNotThrowAnyException();
    }

    @Test
    void losFallosViejosFueraDeLaVentanaNoCuentan() {
        fallar("ana@kala.test", 4);
        reloj.avanzar(Duration.ofMinutes(20));
        limitador.registrarFallo("ana@kala.test");

        assertThatCode(() -> limitador.verificar("ana@kala.test")).doesNotThrowAnyException();
    }

    @Test
    void cadaCorreoTieneSuPropiaCuenta_yLasMayusculasYEspaciosNoLaEluden() {
        fallar("  ANA@kala.test ", 5);

        assertThatThrownBy(() -> limitador.verificar("ana@kala.test")).isInstanceOf(DemasiadosIntentosException.class);
        assertThatCode(() -> limitador.verificar("luis@kala.test")).doesNotThrowAnyException();
    }

    @Test
    void unCorreoNuloOVacioNoRompeNiBloqueaATodos() {
        fallar(null, 10);
        fallar("", 10);

        assertThatCode(() -> limitador.verificar("luis@kala.test")).doesNotThrowAnyException();
        assertThatCode(() -> limitador.verificar(null)).doesNotThrowAnyException();
    }

    @Test
    void laMemoriaNoCreceSinLimite() {
        for (int i = 0; i < 20_000; i++) {
            limitador.registrarFallo("u" + i + "@kala.test");
        }
        reloj.avanzar(Duration.ofHours(2));
        limitador.registrarFallo("ultimo@kala.test");

        assertThat(limitador.entradasEnMemoria()).isLessThan(20_000);
    }

    @Test
    void elNumeroDeEntradasTieneUnTopeAunqueLlegueUnaAvalanchaDeCorreosDistintos() {
        for (int i = 0; i < 60_000; i++) {
            limitador.registrarFallo("u" + i + "@kala.test");
        }

        assertThat(limitador.entradasEnMemoria()).isLessThanOrEqualTo(50_000);
    }

    @Test
    void conElTopeLleno_losCorreosYaRastreadosSiguenLimitandose() {
        fallar("ana@kala.test", 4);
        for (int i = 0; i < 60_000; i++) {
            limitador.registrarFallo("u" + i + "@kala.test");
        }
        limitador.registrarFallo("ana@kala.test");

        assertThatThrownBy(() -> limitador.verificar("ana@kala.test")).isInstanceOf(DemasiadosIntentosException.class);
    }

    @Test
    void laPodaNoSeRepiteEnCadaFalloCuandoLaTablaEstaGrande() {
        for (int i = 0; i < 12_000; i++) {
            limitador.registrarFallo("u" + i + "@kala.test");
        }
        long inicio = System.nanoTime();
        for (int i = 0; i < 2_000; i++) {
            limitador.registrarFallo("v" + i + "@kala.test");
        }

        // 2 000 fallos con la tabla grande no pueden tardar segundos: la poda tiene frecuencia limitada.
        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    void unCorreoEnormeSeRecortaYSeBloqueaIgual() {
        String enorme = "x".repeat(10_000) + "@kala.test";
        fallar(enorme, 5);

        assertThatThrownBy(() -> limitador.verificar(enorme)).isInstanceOf(DemasiadosIntentosException.class);
    }

    @Test
    void rechazaParametrosInvalidos() {
        assertThatThrownBy(() -> new LimitadorIntentosLogin(0, Duration.ofMinutes(1), Duration.ofMinutes(1), reloj))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LimitadorIntentosLogin(5, Duration.ZERO, Duration.ofMinutes(1), reloj))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
