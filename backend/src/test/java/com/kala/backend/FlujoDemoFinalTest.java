package com.kala.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kala.backend.privacidad.dominio.GeneradorDatosDemo;
import com.kala.backend.privacidad.dominio.VentanaSemanal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

// Guion de la demostración final con datos generados (semilla fija) que pasan por el flujo real de la API:
// 1 umbral con 29 y 30 respuestas, 2 precisión según el tamaño, Laplace con baterías, 4 privacidad y 5 presupuesto.
// El escenario 3 (tendencia en ocho semanas) no está aquí: el dashboard consulta una sola ventana a la vez.
// Todo corre dentro de una transacción que se revierte, así que no deja datos en la base.
// Son unas 500 peticiones contra Neon (unos 4 minutos), así que solo corre con KALA_DEMO_FINAL=1:
//   KALA_DEMO_FINAL=1 ./mvnw test -Dtest=FlujoDemoFinalTest
@EnabledIfEnvironmentVariable(named = "KALA_DEMO_FINAL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "KALA_TEST_DB_URL", matches = ".+")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "kala.sesion.secreto=secreto-de-prueba",
        "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class FlujoDemoFinalTest {

    private static final double P = 0.75;
    private static final double PI_REAL = 0.30;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;

    private long uidSiguiente = 5_000_000L;
    private Long pulso;

    private static RequestPostProcessor como(String rol, long uid, Long empresaId, Long equipoId) {
        return jwt().jwt(j -> {
            j.claim("roles", List.of("ROLE_" + rol)).claim("uid", uid);
            if (empresaId != null) {
                j.claim("empresaId", empresaId);
            }
            if (equipoId != null) {
                j.claim("equipoId", equipoId);
            }
        }).authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private Long cuestionario(String codigo) {
        return jdbc.queryForObject("SELECT id FROM cuestionario WHERE codigo = ?", Long.class, codigo);
    }

    private Long item(String codigo, int orden) {
        return jdbc.queryForObject("""
                SELECT i.id FROM item_cuestionario i JOIN cuestionario q ON q.id = i.cuestionario_id
                WHERE q.codigo = ? AND i.orden = ?
                """, Long.class, codigo, orden);
    }

    // Cada consulta es un viaje a Neon: se guarda el resultado porque la configuración vigente no cambia en la prueba.
    private final Map<Long, Long> configuraciones = new java.util.HashMap<>();

    private Long configuracionDe(Long itemId) {
        return configuraciones.computeIfAbsent(itemId, id ->
                jdbc.queryForObject("SELECT configuracion_ldp_id FROM item_cuestionario WHERE id = ?", Long.class, id));
    }

    private void activar(Long empresaId, String codigo, int estado) throws Exception {
        mockMvc.perform(post("/api/empresas/" + empresaId + "/cuestionarios/" + cuestionario(codigo) + "/activar")
                        .with(como("ADMIN", 1L, null, null)))
                .andExpect(status().is(estado));
    }

    private void enviarPulso(long uid, long equipoId, boolean ruidosa) throws Exception {
        Long itemId = pulso;
        mockMvc.perform(post("/api/checkin").with(como("EMPLEADO", uid, null, equipoId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":" + itemId + ",\"configuracionLdpId\":" + configuracionDe(itemId)
                                + ",\"respuestaRuidosa\":" + ruidosa + "}"))
                .andExpect(status().isAccepted());
    }

    private void enviarValor(long uid, long equipoId, Long itemId, double valor) throws Exception {
        mockMvc.perform(post("/api/checkin").with(como("EMPLEADO", uid, null, equipoId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":" + itemId + ",\"configuracionLdpId\":" + configuracionDe(itemId)
                                + ",\"valorRuidoso\":" + valor + "}"))
                .andExpect(status().isAccepted());
    }

    private String dashboard(Long empresaId, Long rrhhId, Long equipoId) throws Exception {
        LocalDate semana = VentanaSemanal.inicioDeVentana(LocalDate.now(ZoneId.of("America/Lima")));
        return mockMvc.perform(get("/api/dashboard/" + equipoId).param("semana", semana.toString())
                        .with(como("RRHH", rrhhId, empresaId, null)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static Map<String, Object> itemDeDashboard(String cuerpo, Long itemId) {
        List<Map<String, Object>> filas = JsonPath.read(cuerpo, "$.items[?(@.itemId==" + itemId + ")]");
        assertThat(filas).hasSize(1);
        return filas.get(0);
    }

    @Test
    void laDemoFinal_umbralPrecisionLaplacePrivacidadYPresupuesto() throws Exception {
        Long empresaId = FilasPadre.empresa(jdbc);
        Long equipoA = FilasPadre.equipo(jdbc, empresaId);
        Long equipoB = FilasPadre.equipo(jdbc, empresaId);
        Long rrhhId = FilasPadre.usuario(jdbc);
        jdbc.update("INSERT INTO personal_empresa (usuario_id, empresa_id, rol) VALUES (?, ?, 'RRHH')", rrhhId, empresaId);

        // Presupuesto real: pulso semanal (5.49) + WHO-5 (3) + PHQ-8 (6) + GAD-7 (3) = 17.49, dentro del máximo de 20.
        for (String codigo : List.of("PULSO", "WHO5", "PHQ8", "GAD7")) {
            activar(empresaId, codigo, 200);
        }

        GeneradorDatosDemo generador = new GeneradorDatosDemo(GeneradorDatosDemo.SEMILLA_DEMO);
        pulso = item("PULSO", 1);

        // --- Escenario 1: con 29 respuestas el dashboard no estima; al llegar la 30 aparece el resultado.
        var equipoDe30 = generador.randomizedResponse(30, PI_REAL, P);
        for (int i = 0; i < 29; i++) {
            enviarPulso(uidSiguiente++, equipoA, equipoDe30.ruidosas().get(i));
        }
        mockMvc.perform(get("/api/dashboard/" + equipoA)
                        .param("semana", VentanaSemanal.inicioDeVentana(LocalDate.now(ZoneId.of("America/Lima"))).toString())
                        .with(como("RRHH", rrhhId, empresaId, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("DATOS_INSUFICIENTES"))
                .andExpect(jsonPath("$.totalRespuestas").value(29))
                .andExpect(jsonPath("$.items[0].proporcionEstimada").doesNotExist());
        enviarPulso(uidSiguiente++, equipoA, equipoDe30.ruidosas().get(29));
        String conTreinta = dashboard(empresaId, rrhhId, equipoA);
        assertThat((String) JsonPath.read(conTreinta, "$.resultado")).isEqualTo("OK");
        double margen30 = ((Number) itemDeDashboard(conTreinta, pulso).get("margenError95")).doubleValue();

        // --- Escenario 2: precisión según el tamaño. 30 personas dan ≈ ±35 puntos; 150 dan ≈ ±16.
        var equipoDe150 = generador.randomizedResponse(150, PI_REAL, P);
        for (Boolean ruidosa : equipoDe150.ruidosas()) {
            enviarPulso(uidSiguiente++, equipoB, ruidosa);
        }
        String conCientoCincuenta = dashboard(empresaId, rrhhId, equipoB);
        Map<String, Object> itemB = itemDeDashboard(conCientoCincuenta, pulso);
        double margen150 = ((Number) itemB.get("margenError95")).doubleValue();
        double estimada150 = ((Number) itemB.get("proporcionEstimada")).doubleValue();
        assertThat(margen30).isBetween(0.30, 0.40);
        assertThat(margen150).isBetween(0.13, 0.19);
        assertThat(margen150).isLessThan(margen30);
        assertThat(Math.abs(estimada150 - PI_REAL)).as("estimación %s frente al valor real %s", estimada150, PI_REAL)
                .isLessThanOrEqualTo(2 * margen150);

        // --- Laplace con baterías: WHO-5 (k = 1) y PHQ-8 (k = 2, un lote por persona) con 150 personas.
        Long who5 = item("WHO5", 1);
        Long phq1 = item("PHQ8", 1);
        Long phq2 = item("PHQ8", 2);
        var animo = generador.laplace(150, 0, 5, 1.0);
        var phq = generador.laplace(150, 0, 3, 1.0);
        long uidPrimero = uidSiguiente;
        for (int i = 0; i < 150; i++) {
            enviarValor(uidSiguiente++, equipoB, who5, animo.ruidosos().get(i));
        }
        for (int i = 0; i < 150; i++) {
            long uid = uidPrimero + i;
            mockMvc.perform(post("/api/checkin/lote").with(como("EMPLEADO", uid, null, equipoB))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"respuestas\":["
                                    + "{\"itemId\":" + phq1 + ",\"configuracionLdpId\":" + configuracionDe(phq1)
                                    + ",\"valorRuidoso\":" + phq.ruidosos().get(i) + "},"
                                    + "{\"itemId\":" + phq2 + ",\"configuracionLdpId\":" + configuracionDe(phq2)
                                    + ",\"valorRuidoso\":" + phq.ruidosos().get(i) + "}]}"))
                    .andExpect(status().isAccepted());
        }
        // Una persona que ya respondió esa batería en la ventana recibe 409; tres ítems con k = 2 reciben 400.
        mockMvc.perform(post("/api/checkin/lote").with(como("EMPLEADO", uidPrimero, null, equipoB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"respuestas\":[{\"itemId\":" + phq1 + ",\"configuracionLdpId\":" + configuracionDe(phq1)
                                + ",\"valorRuidoso\":1.0}]}"))
                .andExpect(status().isConflict());
        Long phq3 = item("PHQ8", 3);
        mockMvc.perform(post("/api/checkin/lote").with(como("EMPLEADO", uidSiguiente++, null, equipoB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"respuestas\":["
                                + "{\"itemId\":" + phq1 + ",\"configuracionLdpId\":" + configuracionDe(phq1) + ",\"valorRuidoso\":1.0},"
                                + "{\"itemId\":" + phq2 + ",\"configuracionLdpId\":" + configuracionDe(phq2) + ",\"valorRuidoso\":1.0},"
                                + "{\"itemId\":" + phq3 + ",\"configuracionLdpId\":" + configuracionDe(phq3) + ",\"valorRuidoso\":1.0}]}"))
                .andExpect(status().isBadRequest());

        String laplace = dashboard(empresaId, rrhhId, equipoB);
        Map<String, Object> filaAnimo = itemDeDashboard(laplace, who5);
        double mediaAnimo = ((Number) filaAnimo.get("mediaEstimada")).doubleValue();
        double margenAnimo = ((Number) filaAnimo.get("margenError95")).doubleValue();
        assertThat(filaAnimo.get("proporcionEstimada")).isNull();
        assertThat(Math.abs(mediaAnimo - animo.mediaReal())).as("WHO-5: %s frente a %s", mediaAnimo, animo.mediaReal())
                .isLessThanOrEqualTo(2 * margenAnimo);
        assertThat(((Number) itemDeDashboard(laplace, phq1).get("mediaEstimada")).doubleValue()).isBetween(0.0, 3.0);

        // --- Escenario 4: privacidad. Solo hay sumas y hashes; ninguna columna ni clave enlaza persona y respuesta.
        List<String> columnas = jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_name IN ('agregado_item', 'sesion_checkin')
                """, String.class);
        assertThat(columnas).noneMatch(c -> c.contains("usuario") || c.contains("persona") || c.contains("email"));
        assertThat(columnas).doesNotContain("creado_en", "timestamp");
        List<String> fkAUsuario = jdbc.queryForList("""
                SELECT DISTINCT tc.table_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name = tc.constraint_name
                WHERE tc.constraint_type = 'FOREIGN KEY'
                  AND tc.table_name IN ('agregado_item', 'sesion_checkin')
                  AND ccu.table_name IN ('usuario', 'empleado_equipo')
                """, String.class);
        assertThat(fkAUsuario).isEmpty();

        // --- Escenario 5: cada consulta de RRHH queda auditada, con datos suficientes o sin ellos (equipo A: la de 29 y la de 30).
        assertThat(jdbc.queryForObject("SELECT count(*) FROM consulta_dashboard WHERE equipo_id = ?", Long.class, equipoA))
                .isEqualTo(2L);

        // --- Escenario 5: una empresa con un presupuesto menor rechaza el cuestionario que lo excede (422).
        Long empresaEstricta = FilasPadre.empresa(jdbc);
        jdbc.update("INSERT INTO presupuesto_privacidad (empresa_id, periodo, epsilon_maximo_persona) VALUES (?, ?, 10)",
                empresaEstricta, java.time.YearMonth.now(ZoneId.of("UTC")).toString());
        activar(empresaEstricta, "PULSO", 200);   // 5.49
        activar(empresaEstricta, "WHO5", 200);    // + 3 = 8.49
        activar(empresaEstricta, "PHQ8", 422);    // + 6 = 14.49 > 10
    }
}
