package com.kala.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

// Sesiones de 30 días de punta a punta con login real: el refresh rota, reutilizar un token usado revoca toda la
// familia, y cerrar sesión, cambiar la clave o desactivar la cuenta dejan los refresh sin efecto.
@EnabledIfEnvironmentVariable(named = "KALA_TEST_DB_URL", matches = ".+")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "kala.sesion.secreto=secreto-de-prueba",
        "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class SesionRefrescoFlujoTest {

    private static final String CLAVE = "una-clave-larga-123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder codificador;

    private record Cuenta(Long id, String email) {
    }

    private Cuenta crearAdmin() {
        String email = "refresh-" + UUID.randomUUID() + "@kala.test";
        Long id = jdbc.queryForObject(
                "INSERT INTO usuario (email, password_hash, nombre) VALUES (?, ?, 'Persona') RETURNING id",
                Long.class, email, codificador.encode(CLAVE));
        jdbc.update("INSERT INTO administrador_plataforma (usuario_id) VALUES (?)", id);
        return new Cuenta(id, email);
    }

    private MvcResult enviar(String ruta, String cuerpo) throws Exception {
        return mockMvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
    }

    private MvcResult login(Cuenta c) throws Exception {
        return enviar("/api/auth/login", "{\"username\":\"" + c.email() + "\",\"password\":\"" + CLAVE + "\"}");
    }

    private int refrescar(String refresh) throws Exception {
        return enviar("/api/auth/refresh", "{\"refreshToken\":\"" + refresh + "\"}").getResponse().getStatus();
    }

    private static String campo(MvcResult r, String ruta) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(), ruta).toString();
    }

    @Test
    void elLoginEntregaUnRefreshDe30Dias_ElRefreshRotaYEntregaUnJwtNuevo() throws Exception {
        Cuenta c = crearAdmin();
        MvcResult login = login(c);
        String refresh1 = campo(login, "$.refreshToken");

        assertThat(login.getResponse().getStatus()).isEqualTo(200);
        assertThat(campo(login, "$.refreshExpiresInSeconds")).isEqualTo("2592000");
        assertThat(refresh1).hasSize(43);
        // En la base solo queda el hash, nunca el token.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_token WHERE token_hash = ?", Long.class, refresh1))
                .isZero();

        MvcResult r2 = enviar("/api/auth/refresh", "{\"refreshToken\":\"" + refresh1 + "\"}");
        assertThat(r2.getResponse().getStatus()).isEqualTo(200);
        String refresh2 = campo(r2, "$.refreshToken");
        assertThat(refresh2).isNotEqualTo(refresh1);
        assertThat(campo(r2, "$.accessToken")).isNotBlank();
        assertThat(campo(r2, "$.tokenType")).isEqualTo("Bearer");
        assertThat(refrescar(refresh2)).isEqualTo(200);
    }

    @Test
    void reutilizarUnTokenYaUsado_revocaTodaLaFamilia_inclusoElNuevo() throws Exception {
        Cuenta c = crearAdmin();
        String refresh1 = campo(login(c), "$.refreshToken");
        MvcResult r2 = enviar("/api/auth/refresh", "{\"refreshToken\":\"" + refresh1 + "\"}");
        String refresh2 = campo(r2, "$.refreshToken");

        // Alguien presenta de nuevo el primero (robado): se rechaza y se revoca la familia completa.
        assertThat(refrescar(refresh1)).isEqualTo(401);
        assertThat(refrescar(refresh2)).isEqualTo(401);
        // La persona legítima vuelve a iniciar sesión con su clave y recupera la sesión.
        assertThat(login(c).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void cerrarSesionRevocaElRefresh_yEsIdempotente() throws Exception {
        Cuenta c = crearAdmin();
        String refresh = campo(login(c), "$.refreshToken");

        assertThat(enviar("/api/auth/logout", "{\"refreshToken\":\"" + refresh + "\"}").getResponse().getStatus()).isEqualTo(204);
        assertThat(enviar("/api/auth/logout", "{\"refreshToken\":\"" + refresh + "\"}").getResponse().getStatus()).isEqualTo(204);
        assertThat(enviar("/api/auth/logout", "{\"refreshToken\":\"no-existe\"}").getResponse().getStatus()).isEqualTo(204);
        assertThat(refrescar(refresh)).isEqualTo(401);
    }

    @Test
    void unTokenInventadoOMalFormado_es401o400() throws Exception {
        assertThat(refrescar("inventado-" + UUID.randomUUID())).isEqualTo(401);
        assertThat(enviar("/api/auth/refresh", "{}").getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void cambiarLaClaveRevocaTodasLasSesiones() throws Exception {
        Cuenta c = crearAdmin();
        MvcResult login = login(c);
        String refresh = campo(login, "$.refreshToken");
        String jwt = campo(login, "$.accessToken");

        int estado = mockMvc.perform(put("/api/usuarios/me/clave").header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveActual\":\"" + CLAVE + "\",\"claveNueva\":\"otra-clave-nueva-2\"}"))
                .andReturn().getResponse().getStatus();

        assertThat(estado).isEqualTo(204);
        assertThat(refrescar(refresh)).isEqualTo(401);
    }

    @Test
    void desactivarLaCuentaRevocaSusRefresh_yUnaCuentaDesactivadaNoRefresca() throws Exception {
        Cuenta objetivo = crearAdmin();
        Cuenta admin = crearAdmin();
        String refresh = campo(login(objetivo), "$.refreshToken");
        String jwtAdmin = campo(login(admin), "$.accessToken");

        int estado = mockMvc.perform(patch("/api/usuarios/" + objetivo.id() + "/desactivar")
                        .header("Authorization", "Bearer " + jwtAdmin))
                .andReturn().getResponse().getStatus();

        assertThat(estado).isEqualTo(204);
        assertThat(refrescar(refresh)).isEqualTo(401);
    }
}
