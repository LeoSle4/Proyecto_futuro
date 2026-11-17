package com.kala.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

// Alta de cuentas de punta a punta con login real contra la rama de pruebas de Neon:
// admin crea RRHH -> RRHH crea un empleado en su equipo (y no en el de otra empresa) -> el empleado cambia su clave.
// Todo corre dentro de una transacción que se revierte al terminar.
@EnabledIfEnvironmentVariable(named = "KALA_TEST_DB_URL", matches = ".+")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "kala.sesion.secreto=secreto-de-prueba",
        "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class GestionUsuariosFlujoTest {

    private static final String CLAVE_ADMIN = "clave-del-admin-123";
    private static final String CLAVE_RRHH = "clave-de-rrhh-1234";
    private static final String CLAVE_EMPLEADO = "clave-del-empleado-1";
    private static final String CLAVE_NUEVA = "clave-nueva-del-empleado-2";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder codificador;

    private static RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    private String loginStatus(String email, String clave, int esperado) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + email + "\",\"password\":\"" + clave + "\"}"))
                .andExpect(status().is(esperado))
                .andReturn().getResponse().getContentAsString();
    }

    private String token(String email, String clave) throws Exception {
        return JsonPath.read(loginStatus(email, clave, 200), "$.accessToken");
    }

    private String cuerpoPersonal(String email, String clave) {
        return "{\"email\":\"" + email + "\",\"nombre\":\"Ana Ríos\",\"clave\":\"" + clave + "\",\"rol\":\"RRHH\"}";
    }

    private String cuerpoEmpleado(String email, String clave, Long equipoId) {
        return "{\"email\":\"" + email + "\",\"nombre\":\"Luis Paz\",\"clave\":\"" + clave + "\",\"equipoId\":" + equipoId + "}";
    }

    @Test
    void altaDePersonalYEmpleados_aislamientoPorEmpresa_yCambioDeClave() throws Exception {
        String sufijo = UUID.randomUUID().toString();
        String emailAdmin = "admin-" + sufijo + "@kala.test";
        String emailRrhh = "rrhh-" + sufijo + "@kala.test";
        String emailEmpleado = "empleado-" + sufijo + "@kala.test";
        Long empresaA = FilasPadre.empresa(jdbc);
        Long equipoA = FilasPadre.equipo(jdbc, empresaA);
        Long empresaB = FilasPadre.empresa(jdbc);
        Long equipoB = FilasPadre.equipo(jdbc, empresaB);
        Long adminId = jdbc.queryForObject(
                "INSERT INTO usuario (email, password_hash, nombre) VALUES (?, ?, 'Admin') RETURNING id",
                Long.class, emailAdmin, codificador.encode(CLAVE_ADMIN));
        jdbc.update("INSERT INTO administrador_plataforma (usuario_id) VALUES (?)", adminId);
        String tokenAdmin = token(emailAdmin, CLAVE_ADMIN);

        // El ADMIN crea al RRHH de la empresa A (el correo se normaliza y la clave queda cifrada).
        String respuestaAlta = mockMvc.perform(post("/api/admin/empresas/" + empresaA + "/personal").with(bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPersonal(emailRrhh.toUpperCase(), CLAVE_RRHH)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat(respuestaAlta).doesNotContain(CLAVE_RRHH).doesNotContain("$2");
        String hash = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE email = ?", String.class, emailRrhh);
        assertThat(hash).startsWith("$2").doesNotContain(CLAVE_RRHH);

        // Un correo repetido (aunque cambie la capitalización) es 409; una empresa inexistente es 404.
        mockMvc.perform(post("/api/admin/empresas/" + empresaA + "/personal").with(bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoPersonal(emailRrhh, CLAVE_RRHH)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/admin/empresas/" + (empresaB + 999_999) + "/personal").with(bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoPersonal("otro-" + emailRrhh, CLAVE_RRHH)))
                .andExpect(status().isNotFound());

        // El RRHH entra con su cuenta nueva y crea un empleado en un equipo de SU empresa.
        String tokenRrhh = token(emailRrhh, CLAVE_RRHH);
        mockMvc.perform(post("/api/empleados").with(bearer(tokenRrhh)).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEmpleado(emailEmpleado, CLAVE_EMPLEADO, equipoA)))
                .andExpect(status().isCreated());
        // El equipo de otra empresa responde 404 y no crea nada.
        mockMvc.perform(post("/api/empleados").with(bearer(tokenRrhh)).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEmpleado("ajeno-" + emailEmpleado, CLAVE_EMPLEADO, equipoB)))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE email = ?", Long.class, "ajeno-" + emailEmpleado))
                .isZero();
        // Una clave corta es 400 y el RRHH no puede crear personal (solo el ADMIN).
        mockMvc.perform(post("/api/empleados").with(bearer(tokenRrhh)).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEmpleado("corta-" + emailEmpleado, "corta", equipoA)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/empresas/" + empresaA + "/personal").with(bearer(tokenRrhh))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoPersonal("x-" + emailRrhh, CLAVE_RRHH)))
                .andExpect(status().isForbidden());

        // El empleado entra, no puede crear cuentas y cambia su clave.
        String tokenEmpleado = token(emailEmpleado, CLAVE_EMPLEADO);
        mockMvc.perform(post("/api/empleados").with(bearer(tokenEmpleado)).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEmpleado("otro-" + emailEmpleado, CLAVE_EMPLEADO, equipoA)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/usuarios/me/clave").with(bearer(tokenEmpleado)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveActual\":\"incorrecta-1234567\",\"claveNueva\":\"" + CLAVE_NUEVA + "\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/usuarios/me/clave").with(bearer(tokenEmpleado)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveActual\":\"" + CLAVE_EMPLEADO + "\",\"claveNueva\":\"" + CLAVE_NUEVA + "\"}"))
                .andExpect(status().isNoContent());

        // La clave vieja deja de servir y la nueva sí.
        loginStatus(emailEmpleado, CLAVE_EMPLEADO, 401);
        assertThat(JsonPath.<String>read(loginStatus(emailEmpleado, CLAVE_NUEVA, 200), "$.tokenType")).isEqualTo("Bearer");
    }

    private Long usuario(String email) {
        return jdbc.queryForObject(
                "INSERT INTO usuario (email, password_hash, nombre) VALUES (?, ?, 'Persona') RETURNING id",
                Long.class, email, codificador.encode(CLAVE_EMPLEADO));
    }

    private int patch(String ruta, String token) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(ruta).with(bearer(token)))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void desactivarUnaCuentaBloqueaElLogin_reactivarLoDevuelve_yLasEmpresasSeAislan() throws Exception {
        String sufijo = UUID.randomUUID().toString();
        Long empresaA = FilasPadre.empresa(jdbc);
        Long equipoA = FilasPadre.equipo(jdbc, empresaA);
        Long empresaB = FilasPadre.empresa(jdbc);
        String emailAdmin = "admin-" + sufijo + "@kala.test";
        String emailRrhhA = "rrhha-" + sufijo + "@kala.test";
        String emailRrhhB = "rrhhb-" + sufijo + "@kala.test";
        String emailEmpleado = "emp-" + sufijo + "@kala.test";
        Long adminId = usuario(emailAdmin);
        jdbc.update("INSERT INTO administrador_plataforma (usuario_id) VALUES (?)", adminId);
        Long rrhhA = usuario(emailRrhhA);
        jdbc.update("INSERT INTO personal_empresa (usuario_id, empresa_id, rol) VALUES (?, ?, 'RRHH')", rrhhA, empresaA);
        Long rrhhB = usuario(emailRrhhB);
        jdbc.update("INSERT INTO personal_empresa (usuario_id, empresa_id, rol) VALUES (?, ?, 'RRHH')", rrhhB, empresaB);
        Long empleado = usuario(emailEmpleado);
        jdbc.update("INSERT INTO empleado_equipo (usuario_id, equipo_id) VALUES (?, ?)", empleado, equipoA);
        String tokenAdmin = token(emailAdmin, CLAVE_EMPLEADO);
        String tokenA = token(emailRrhhA, CLAVE_EMPLEADO);
        String tokenB = token(emailRrhhB, CLAVE_EMPLEADO);

        // El RRHH de otra empresa, y el de la misma sobre una cuenta que no es empleado, reciben 404.
        assertThat(patch("/api/usuarios/" + empleado + "/desactivar", tokenB)).isEqualTo(404);
        assertThat(patch("/api/usuarios/" + adminId + "/desactivar", tokenA)).isEqualTo(404);
        loginStatus(emailEmpleado, CLAVE_EMPLEADO, 200);

        // El RRHH de la empresa desactiva al empleado: ya no puede entrar. Reactivarlo lo devuelve.
        assertThat(patch("/api/usuarios/" + empleado + "/desactivar", tokenA)).isEqualTo(204);
        assertThat(patch("/api/usuarios/" + empleado + "/desactivar", tokenA)).isEqualTo(204);
        loginStatus(emailEmpleado, CLAVE_EMPLEADO, 401);
        assertThat(patch("/api/usuarios/" + empleado + "/activar", tokenA)).isEqualTo(204);
        loginStatus(emailEmpleado, CLAVE_EMPLEADO, 200);

        // El ADMIN desactiva a un RRHH, pero no a sí mismo.
        assertThat(patch("/api/usuarios/" + rrhhA + "/desactivar", tokenAdmin)).isEqualTo(204);
        loginStatus(emailRrhhA, CLAVE_EMPLEADO, 401);
        assertThat(patch("/api/usuarios/" + adminId + "/desactivar", tokenAdmin)).isEqualTo(400);
        loginStatus(emailAdmin, CLAVE_EMPLEADO, 200);
        assertThat(patch("/api/usuarios/" + (adminId + 9_999_999) + "/desactivar", tokenAdmin)).isEqualTo(404);
    }
}
