package com.kala.backend.checkin.infraestructura.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.kala.backend.checkin.aplicacion.puerto.ItemsCuestionario.ItemCuestionarioInfo;
import com.kala.backend.privacidad.dominio.ParametrosLdp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

// item_cuestionario es de otro módulo: se simula con una tabla TEMPORAL que se descarta con la transacción
// del test, de modo que no deja nada en la rama de Neon ni choca con la migración real.
@EnabledIfEnvironmentVariable(named = "KALA_TEST_DB_URL", matches = ".+")
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Sql(statements = {
        "CREATE TEMPORARY TABLE item_cuestionario (id BIGINT, cuestionario_id BIGINT, configuracion_ldp_id BIGINT, escala_id BIGINT)",
        "CREATE TEMPORARY TABLE escala_respuesta (id BIGINT, valor_min SMALLINT, valor_max SMALLINT)",
        "INSERT INTO escala_respuesta (id, valor_min, valor_max) VALUES (5, 0, 3)",
        "CREATE TEMPORARY TABLE cuestionario (id BIGINT, cadencia_dias SMALLINT, items_por_envio SMALLINT)",
        "CREATE TEMPORARY TABLE configuracion_ldp (id BIGINT, mecanismo VARCHAR(25), probabilidad_verdad DECIMAL(4,3), epsilon DECIMAL(6,3))",
        "INSERT INTO configuracion_ldp (id, mecanismo, epsilon) VALUES (2, 'LAPLACE', 1.500)",
        "INSERT INTO cuestionario (id, cadencia_dias, items_por_envio) VALUES (3, 14, 2)",
        "INSERT INTO item_cuestionario (id, cuestionario_id, configuracion_ldp_id, escala_id) VALUES (9001, 3, 2, 5), (9002, 3, 2, 5), (9003, 3, 2, 5)"
})
class ItemCuestionarioLectorTest {

    @Autowired
    private JdbcClient jdbc;

    @Test
    void itemExistente_devuelveCuestionarioConfiguracionMecanismoCadenciaYK() {
        assertThat(new ItemCuestionarioLector(jdbc).buscar(9001L))
                .contains(new ItemCuestionarioInfo(3L, 2L, "LAPLACE", 14, 2, 3,
                        new com.kala.backend.privacidad.dominio.ParametrosLdp(null, new java.math.BigDecimal("1.500")), 0, 3));
    }

    @Test
    void itemInexistente_devuelveVacio() {
        assertThat(new ItemCuestionarioLector(jdbc).buscar(424242L)).isEmpty();
    }
}
