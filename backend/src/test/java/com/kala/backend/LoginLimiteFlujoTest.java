package com.kala.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.transaction.annotation.Transactional;

// Límite de inicios de sesión de punta a punta: cinco fallos bloquean ese correo (429 con Retry-After) aunque la clave
// ya sea correcta, y otro correo sigue entrando. Usa correos únicos porque el limitador vive en memoria de la aplicación.
@EnabledIfEnvironmentVariable(named = "KALA_TEST_DB_URL", matches = ".+")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "kala.sesion.secreto=secreto-de-prueba",
        "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class LoginLimiteFlujoTest {

    private static final String CLAVE = "una-clave-larga-123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder codificador;

    private String crearAdmin() {
        String email = "limite-" + UUID.randomUUID() + "@kala.test";
        Long id = jdbc.queryForObject(
                "INSERT INTO usuario (email, password_hash, nombre) VALUES (?, ?, 'Persona') RETURNING id",
                Long.class, email, codificador.encode(CLAVE));
        jdbc.update("INSERT INTO administrador_plataforma (usuario_id) VALUES (?)", id);
        return email;
    }

    private int login(String email, String clave) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + email + "\",\"password\":\"" + clave + "\"}"))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void cincoFallosBloqueanElCorreo_yOtroCorreoSigueEntrando() throws Exception {
        String bloqueado = crearAdmin();
        String libre = crearAdmin();

        for (int i = 0; i < 5; i++) {
            assertThat(login(bloqueado, "equivocada-123456")).isEqualTo(401);
        }
        // Aun con la clave correcta, el bloqueo manda; y no se calcula el BCrypt.
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + bloqueado.toUpperCase() + "\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));

        assertThat(login(libre, CLAVE)).isEqualTo(200);
    }

    @Test
    void unCorreoQueNoExisteSeLimitaIgualQueUnoQueSi_sinRevelarCualEs() throws Exception {
        String fantasma = "fantasma-" + UUID.randomUUID() + "@kala.test";

        for (int i = 0; i < 5; i++) {
            assertThat(login(fantasma, "equivocada-123456")).isEqualTo(401);
        }
        assertThat(login(fantasma, "equivocada-123456")).isEqualTo(429);
    }

    @Test
    void unInicioDeSesionExitosoReiniciaLaCuenta() throws Exception {
        String email = crearAdmin();

        for (int ronda = 0; ronda < 3; ronda++) {
            for (int i = 0; i < 4; i++) {
                assertThat(login(email, "equivocada-123456")).isEqualTo(401);
            }
            assertThat(login(email, CLAVE)).isEqualTo(200);
        }
    }
}
