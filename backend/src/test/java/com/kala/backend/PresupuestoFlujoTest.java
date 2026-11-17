package com.kala.backend;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.hamcrest.Matchers;
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

// Presupuesto de privacidad y activación/desactivación de cuestionarios de punta a punta contra la rama de pruebas
// de Neon: el ADMIN fija el máximo, activa hasta donde alcanza, apaga uno para hacer lugar y el empleado solo recibe lo
// activo. Todo corre dentro de una transacción que se revierte.
@EnabledIfEnvironmentVariable(named = "KALA_TEST_DB_URL", matches = ".+")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "kala.sesion.secreto=secreto-de-prueba",
        "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class PresupuestoFlujoTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private jakarta.persistence.EntityManager em;

    private static RequestPostProcessor como(String rol, Long empresaId, Long equipoId) {
        return jwt().jwt(j -> {
            j.claim("roles", List.of("ROLE_" + rol)).claim("uid", 1L);
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

    private String ruta(Long empresa, String codigo, String accion) {
        return "/api/empresas/" + empresa + "/cuestionarios/" + cuestionario(codigo) + "/" + accion;
    }

    @Test
    void presupuestoYDesactivacion_sePuedenGestionarSinTocarLaBase() throws Exception {
        Long empresa = FilasPadre.empresa(jdbc);
        Long otraEmpresa = FilasPadre.empresa(jdbc);
        Long equipo = FilasPadre.equipo(jdbc, empresa);

        // Sin presupuesto fijado rige el valor por defecto (20) y todavía no se gasta nada.
        mockMvc.perform(get("/api/empresas/" + empresa + "/presupuesto").with(como("ADMIN", null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.epsilonMaximo").value(20.0))
                .andExpect(jsonPath("$.epsilonMensual").value(0.0));

        // El pulso (5.49) y el WHO-5 (3) caben; juntos gastan 8.49.
        mockMvc.perform(post(ruta(empresa, "PULSO", "activar")).with(como("ADMIN", null, null))).andExpect(status().isOk());
        mockMvc.perform(post(ruta(empresa, "WHO5", "activar")).with(como("ADMIN", null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.epsilonMensual").value(Matchers.closeTo(8.49, 0.01)));

        // Fijar un máximo menor que lo ya activo se rechaza; uno de 10 se acepta y queda como presupuesto vigente.
        mockMvc.perform(put("/api/empresas/" + empresa + "/presupuesto").with(como("ADMIN", null, null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"epsilonMaximoPersona\": 8}"))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(put("/api/empresas/" + empresa + "/presupuesto").with(como("ADMIN", null, null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"epsilonMaximoPersona\": 10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.epsilonMaximo").value(10.0));
        mockMvc.perform(put("/api/empresas/" + empresa + "/presupuesto").with(como("ADMIN", null, null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"epsilonMaximoPersona\": 10.5}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/empresas/" + empresa + "/presupuesto").with(como("ADMIN", null, null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"epsilonMaximoPersona\": 10}"))
                .andExpect(status().isOk());
        em.flush(); // Hibernate escribe al final de la transacción: se vuelca antes de contar por JDBC
        Long filas = jdbc.queryForObject("SELECT count(*) FROM presupuesto_privacidad WHERE empresa_id = ?", Long.class, empresa);
        org.assertj.core.api.Assertions.assertThat(filas).isEqualTo(1L);

        // Con máximo 10, el PHQ-8 (6) ya no cabe; al apagar el WHO-5 sigue sin caber (5.49 + 6 > 10); con 12 sí.
        mockMvc.perform(post(ruta(empresa, "PHQ8", "activar")).with(como("ADMIN", null, null)))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(post(ruta(empresa, "WHO5", "desactivar")).with(como("ADMIN", null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.epsilonMensual").value(Matchers.closeTo(5.49, 0.01)));
        mockMvc.perform(post(ruta(empresa, "WHO5", "desactivar")).with(como("ADMIN", null, null)))
                .andExpect(status().isOk());
        mockMvc.perform(post(ruta(empresa, "PHQ8", "activar")).with(como("ADMIN", null, null)))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(put("/api/empresas/" + empresa + "/presupuesto").with(como("ADMIN", null, null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"epsilonMaximoPersona\": 12}"))
                .andExpect(status().isOk());
        mockMvc.perform(post(ruta(empresa, "PHQ8", "activar")).with(como("ADMIN", null, null))).andExpect(status().isOk());

        // El empleado solo recibe lo activo: el pulso y el PHQ-8, no el WHO-5 apagado.
        mockMvc.perform(get("/api/cuestionarios/pendientes").with(como("EMPLEADO", empresa, equipo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo").value(Matchers.containsInAnyOrder("PULSO", "PHQ8")));

        // Aislamiento: RRHH ve el presupuesto de su empresa y no el de otra; solo el ADMIN lo cambia.
        mockMvc.perform(get("/api/empresas/" + empresa + "/presupuesto").with(como("RRHH", empresa, null)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.epsilonMaximo").value(12.0));
        mockMvc.perform(get("/api/empresas/" + otraEmpresa + "/presupuesto").with(como("RRHH", empresa, null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/empresas/" + empresa + "/presupuesto").with(como("RRHH", empresa, null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"epsilonMaximoPersona\": 999}"))
                .andExpect(status().isForbidden());
    }
}
