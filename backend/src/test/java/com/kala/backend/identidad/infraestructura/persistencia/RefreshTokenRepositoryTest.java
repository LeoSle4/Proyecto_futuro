package com.kala.backend.identidad.infraestructura.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.kala.backend.FilasPadre;
import com.kala.backend.identidad.dominio.RefreshToken;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// Contra la rama de pruebas de Neon: las consultas que deben ser atómicas (marcar usado, revocar) y la purga.
@EnabledIfEnvironmentVariable(named = "KALA_TEST_DB_URL", matches = ".+")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RefreshTokenRepositoryTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 17, 15, 0);

    @Autowired
    private RefreshTokenRepository repo;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager em;

    private RefreshToken nuevo(Long usuario, String familia, String hash, LocalDateTime expira) {
        return repo.saveAndFlush(new RefreshToken(usuario, familia, hash, AHORA.minusDays(1), expira));
    }

    @Test
    void seBuscaPorElHashYSoloElHashEstaGuardado() {
        Long usuario = FilasPadre.usuario(jdbc);
        nuevo(usuario, "fam", "a".repeat(64), AHORA.plusDays(30));

        assertThat(repo.porHash("a".repeat(64))).isPresent();
        assertThat(repo.porHash("b".repeat(64))).isEmpty();
        assertThat(jdbc.queryForObject("SELECT token_hash FROM refresh_token WHERE usuario_id = ?", String.class, usuario))
                .hasSize(64);
    }

    @Test
    void marcarUsadoEsAtomico_soloLaPrimeraVezGana() {
        Long usuario = FilasPadre.usuario(jdbc);
        RefreshToken t = nuevo(usuario, "fam", "c".repeat(64), AHORA.plusDays(30));

        assertThat(repo.marcarUsado(t.getId(), AHORA)).isTrue();
        assertThat(repo.marcarUsado(t.getId(), AHORA)).isFalse();
    }

    @Test
    void unTokenRevocadoNoSePuedeMarcarUsado() {
        Long usuario = FilasPadre.usuario(jdbc);
        RefreshToken t = nuevo(usuario, "fam-r", "d".repeat(64), AHORA.plusDays(30));
        repo.revocarFamilia("fam-r", AHORA);

        assertThat(repo.marcarUsado(t.getId(), AHORA)).isFalse();
    }

    @Test
    void revocarUnaFamiliaNoTocaLasOtras_yRevocarDeUsuarioRevocaSoloLasSuyas() {
        Long ana = FilasPadre.usuario(jdbc);
        Long luis = FilasPadre.usuario(jdbc);
        nuevo(ana, "f-ana-1", "1".repeat(64), AHORA.plusDays(30));
        nuevo(ana, "f-ana-1", "2".repeat(64), AHORA.plusDays(30));
        nuevo(ana, "f-ana-2", "3".repeat(64), AHORA.plusDays(30));
        nuevo(luis, "f-luis", "4".repeat(64), AHORA.plusDays(30));

        assertThat(repo.revocarFamilia("f-ana-1", AHORA)).isEqualTo(2);
        assertThat(repo.revocarFamilia("f-ana-1", AHORA)).isZero();
        assertThat(repo.revocarDeUsuario(ana, AHORA)).isEqualTo(1);
        em.clear();

        assertThat(repo.porHash("4".repeat(64))).get().extracting(RefreshToken::getRevocadoEn).isNull();
        assertThat(repo.porHash("3".repeat(64))).get().extracting(RefreshToken::getRevocadoEn).isNotNull();
    }

    @Test
    void laPurgaBorraSoloLosVencidosAntesDelLimite() {
        Long usuario = FilasPadre.usuario(jdbc);
        nuevo(usuario, "f", "5".repeat(64), AHORA.minusDays(10));
        nuevo(usuario, "f", "6".repeat(64), AHORA.minusDays(3));
        nuevo(usuario, "f", "7".repeat(64), AHORA.plusDays(3));

        assertThat(repo.borrarExpiradosAntesDe(AHORA.minusDays(7))).isEqualTo(1);
        em.clear();

        assertThat(repo.porHash("5".repeat(64))).isEmpty();
        assertThat(repo.porHash("6".repeat(64))).isPresent();
        assertThat(repo.porHash("7".repeat(64))).isPresent();
    }
}
