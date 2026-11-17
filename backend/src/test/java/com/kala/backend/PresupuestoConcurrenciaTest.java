package com.kala.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.kala.backend.instrumentos.aplicacion.ActivacionCuestionarioService;
import com.kala.backend.instrumentos.dominio.PresupuestoExcedidoException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

// Sin transacción de prueba a propósito: dos activaciones simultáneas que por separado caben en el presupuesto pero
// juntas lo exceden no pueden tener éxito las dos. Con máximo 10, el pulso (5.49) y el PHQ-8 (6) caben solos, no juntos.
// Escribe filas reales en la rama de pruebas y las borra al terminar.
@EnabledIfEnvironmentVariable(named = "KALA_TEST_DB_URL", matches = ".+")
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "kala.sesion.secreto=secreto-de-prueba",
        "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "kala.retencion.habilitada=false"
})
class PresupuestoConcurrenciaTest {

    private static final int RONDAS = 6;

    @Autowired
    private ActivacionCuestionarioService activacion;
    @Autowired
    private JdbcTemplate jdbc;

    private Long empresaId;

    @AfterEach
    void limpiar() {
        if (empresaId != null) {
            jdbc.update("DELETE FROM empresa_cuestionario WHERE empresa_id = ?", empresaId);
            jdbc.update("DELETE FROM presupuesto_privacidad WHERE empresa_id = ?", empresaId);
            jdbc.update("DELETE FROM empresa WHERE id = ?", empresaId);
        }
    }

    private Long cuestionario(String codigo) {
        return jdbc.queryForObject("SELECT id FROM cuestionario WHERE codigo = ?", Long.class, codigo);
    }

    private boolean intentar(Long cuestionarioId) {
        try {
            activacion.activar(empresaId, cuestionarioId);
            return true;
        } catch (PresupuestoExcedidoException e) {
            return false;
        }
    }

    @Test
    void dosActivacionesSimultaneasQueJuntasExcedenElPresupuesto_nuncaTienenExitoLasDos() throws Exception {
        empresaId = FilasPadre.empresa(jdbc);
        activacion.fijarPresupuesto(empresaId, new BigDecimal("10"));
        Long pulso = cuestionario("PULSO");
        Long phq8 = cuestionario("PHQ8");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int ronda = 0; ronda < RONDAS; ronda++) {
                jdbc.update("DELETE FROM empresa_cuestionario WHERE empresa_id = ?", empresaId);
                CyclicBarrier salida = new CyclicBarrier(2);
                List<Future<Boolean>> resultados = new ArrayList<>();
                for (Long id : List.of(pulso, phq8)) {
                    Callable<Boolean> tarea = () -> {
                        salida.await();
                        return intentar(id);
                    };
                    resultados.add(pool.submit(tarea));
                }
                int exitos = 0;
                for (Future<Boolean> f : resultados) {
                    if (f.get()) {
                        exitos++;
                    }
                }

                assertThat(exitos).as("ronda %s: solo una activación puede caber", ronda).isEqualTo(1);
                assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM empresa_cuestionario WHERE empresa_id = ? AND activo", Long.class, empresaId))
                        .isEqualTo(1L);
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
