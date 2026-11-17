package com.kala.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kala.backend.identidad.aplicacion.RefrescoDeSesionService;
import com.kala.backend.identidad.dominio.CredencialesInvalidasException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

// Sin transacción de prueba a propósito: comprueba que la revocación de la familia por reutilización QUEDA GUARDADA
// aunque el servicio lance 401 (si la excepción deshiciera la transacción, el robo de un token no se detectaría).
// Escribe filas reales en la rama de pruebas y las borra al terminar.
@EnabledIfEnvironmentVariable(named = "KALA_TEST_DB_URL", matches = ".+")
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "kala.sesion.secreto=secreto-de-prueba",
        "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "kala.retencion.habilitada=false"
})
class RefrescoRevocacionPersistenteTest {

    @Autowired
    private RefrescoDeSesionService refresco;
    @Autowired
    private JdbcTemplate jdbc;

    private Long usuarioId;

    @AfterEach
    void limpiar() {
        if (usuarioId != null) {
            jdbc.update("DELETE FROM refresh_token WHERE usuario_id = ?", usuarioId);
            jdbc.update("DELETE FROM usuario WHERE id = ?", usuarioId);
        }
    }

    @Test
    void reutilizarUnTokenUsado_dejaRevocadaLaFamiliaEnLaBase_auntqueLaLlamadaFalle() {
        usuarioId = jdbc.queryForObject(
                "INSERT INTO usuario (email, password_hash, nombre) VALUES (?, 'x', 'Persona') RETURNING id",
                Long.class, "persistente-" + UUID.randomUUID() + "@kala.test");
        String primero = refresco.iniciar(usuarioId).refreshToken();
        String segundo = refresco.rotar(primero).refreshToken();

        assertThatThrownBy(() -> refresco.rotar(primero)).isInstanceOf(CredencialesInvalidasException.class);

        // El segundo (legítimo) también quedó revocado: la revocación sobrevivió a la excepción.
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM refresh_token WHERE usuario_id = ? AND revocado_en IS NOT NULL", Long.class, usuarioId))
                .isEqualTo(2L);
        assertThatThrownBy(() -> refresco.rotar(segundo)).isInstanceOf(CredencialesInvalidasException.class);
    }
}
