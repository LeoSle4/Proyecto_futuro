package com.kala.backend.instrumentos.infraestructura.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

// Comprueba lo que dejan las migraciones V6 y V9: el pulso y las tres baterias con sus escalas y su epsilon.
@EnabledIfEnvironmentVariable(named = "KALA_TEST_DB_URL", matches = ".+")
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SeedBateriasTest {

    @Autowired
    private JdbcClient jdbc;

    private Map<String, Integer> itemsPorCuestionario() {
        return jdbc.sql("""
                        SELECT q.codigo, COUNT(i.id) AS n
                        FROM cuestionario q LEFT JOIN item_cuestionario i ON i.cuestionario_id = q.id
                        GROUP BY q.codigo
                        """)
                .query((rs, fila) -> Map.entry(rs.getString("codigo"), rs.getInt("n")))
                .list().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Test
    void estanElPulsoYLasTresBateriasConSusItems() {
        assertThat(itemsPorCuestionario())
                .containsEntry("PULSO", 1)
                .containsEntry("WHO5", 5)
                .containsEntry("PHQ8", 8)
                .containsEntry("GAD7", 7);
    }

    @Test
    void cadenciaYKDeCadaBateria() {
        List<String> filas = jdbc.sql("""
                        SELECT codigo || ':' || cadencia_dias || ':' || COALESCE(items_por_envio::text, 'null')
                        FROM cuestionario WHERE codigo IN ('WHO5','PHQ8','GAD7') ORDER BY codigo
                        """)
                .query(String.class).list();

        assertThat(filas).containsExactly("GAD7:14:1", "PHQ8:14:2", "WHO5:14:1");
    }

    @Test
    void lasBateriasUsanLaplaceConEpsilonUnoYLaEscalaCorrecta() {
        List<String> filas = jdbc.sql("""
                        SELECT DISTINCT q.codigo || ':' || c.mecanismo || ':' || c.epsilon || ':' || e.valor_min || '-' || e.valor_max
                        FROM item_cuestionario i
                        JOIN cuestionario q ON q.id = i.cuestionario_id
                        JOIN configuracion_ldp c ON c.id = i.configuracion_ldp_id
                        JOIN escala_respuesta e ON e.id = i.escala_id
                        WHERE q.codigo IN ('WHO5','PHQ8','GAD7') ORDER BY 1
                        """)
                .query(String.class).list();

        assertThat(filas).containsExactly(
                "GAD7:LAPLACE:1.000:0-3", "PHQ8:LAPLACE:1.000:0-3", "WHO5:LAPLACE:1.000:0-5");
    }

    @Test
    void lasEscalasTienenUnaEtiquetaPorValor() {
        List<String> filas = jdbc.sql("""
                        SELECT e.codigo || ':' || COUNT(o.valor)
                        FROM escala_respuesta e JOIN opcion_escala o ON o.escala_id = e.id
                        GROUP BY e.codigo ORDER BY e.codigo
                        """)
                .query(String.class).list();

        assertThat(filas).contains("BINARIA:2", "FRECUENCIA_0_3:4", "WHO5_0_5:6");
    }
}
