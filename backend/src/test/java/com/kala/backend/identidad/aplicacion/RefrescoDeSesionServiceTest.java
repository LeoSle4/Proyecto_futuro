package com.kala.backend.identidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kala.backend.identidad.aplicacion.puerto.RefreshTokens;
import com.kala.backend.identidad.dominio.CredencialesInvalidasException;
import com.kala.backend.identidad.dominio.RefreshToken;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RefrescoDeSesionServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-17T15:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 17, 15, 0);

    private RefreshTokens tokens;
    private RefrescoDeSesionService service;

    @BeforeEach
    void setUp() {
        tokens = mock(RefreshTokens.class);
        when(tokens.guardar(any())).thenAnswer(i -> i.getArgument(0));
        service = new RefrescoDeSesionService(tokens, 30, RELOJ);
    }

    private RefreshToken guardado(String plano, String familia, LocalDateTime expira) {
        RefreshToken t = new RefreshToken(7L, familia, TokenOpaco.hash(plano), AHORA.minusDays(1), expira);
        org.springframework.test.util.ReflectionTestUtils.setField(t, "id", 100L);
        when(tokens.porHash(TokenOpaco.hash(plano))).thenReturn(Optional.of(t));
        return t;
    }

    // ---- iniciar ----

    @Test
    void iniciar_guardaSoloElHash_conVencimientoA30Dias_yDevuelveElTokenEnClaro() {
        var sesion = service.iniciar(7L);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokens).guardar(captor.capture());
        RefreshToken guardado = captor.getValue();
        assertThat(sesion.usuarioId()).isEqualTo(7L);
        assertThat(sesion.refreshToken()).hasSize(43);
        assertThat(guardado.getTokenHash()).isEqualTo(TokenOpaco.hash(sesion.refreshToken()))
                .isNotEqualTo(sesion.refreshToken());
        assertThat(guardado.getExpiraEn()).isEqualTo(AHORA.plusDays(30));
        assertThat(guardado.getFamilia()).isNotBlank();
        assertThat(sesion.expiraEnSegundos()).isEqualTo(30L * 24 * 3600);
    }

    // ---- rotar ----

    @Test
    void rotar_marcaElViejoComoUsado_yEmiteUnoNuevoDeLaMismaFamiliaConOtros30Dias() {
        RefreshToken viejo = guardado("viejo", "fam-1", AHORA.plusDays(3));
        when(tokens.marcarUsado(100L, AHORA)).thenReturn(true);

        var sesion = service.rotar("viejo");

        assertThat(sesion.usuarioId()).isEqualTo(7L);
        assertThat(sesion.refreshToken()).isNotEqualTo("viejo").hasSize(43);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokens).guardar(captor.capture());
        assertThat(captor.getValue().getFamilia()).isEqualTo(viejo.getFamilia());
        assertThat(captor.getValue().getExpiraEn()).isEqualTo(AHORA.plusDays(30));
    }

    @Test
    void rotar_conUnTokenDesconocidoVacioOEnorme_es401YNoToca_nada() {
        when(tokens.porHash(any())).thenReturn(Optional.empty());

        for (String malo : new String[] {"no-existe", "", "   ", null, "x".repeat(500)}) {
            assertThatThrownBy(() -> service.rotar(malo)).isInstanceOf(CredencialesInvalidasException.class);
        }
        verify(tokens, never()).marcarUsado(anyLong(), any());
        verify(tokens, never()).guardar(any());
    }

    @Test
    void rotar_conUnTokenVencido_es401() {
        guardado("vencido", "fam-1", AHORA.minusSeconds(1));

        assertThatThrownBy(() -> service.rotar("vencido")).isInstanceOf(CredencialesInvalidasException.class);
        verify(tokens, never()).guardar(any());
    }

    @Test
    void rotar_conUnTokenRevocado_es401() {
        RefreshToken t = guardado("revocado", "fam-1", AHORA.plusDays(3));
        t.revocar(AHORA.minusHours(1));

        assertThatThrownBy(() -> service.rotar("revocado")).isInstanceOf(CredencialesInvalidasException.class);
        verify(tokens, never()).marcarUsado(anyLong(), any());
    }

    @Test
    void rotar_reutilizandoUnTokenYaUsado_revocaTodaLaFamilia_yEs401() {
        RefreshToken t = guardado("usado", "fam-9", AHORA.plusDays(3));
        t.marcarUsado(AHORA.minusMinutes(5));

        assertThatThrownBy(() -> service.rotar("usado")).isInstanceOf(CredencialesInvalidasException.class);

        verify(tokens).revocarFamilia("fam-9", AHORA);
        verify(tokens, never()).guardar(any());
    }

    @Test
    void rotar_siPierdeLaCarreraAlMarcarloUsado_revocaLaFamilia_yEs401() {
        guardado("carrera", "fam-5", AHORA.plusDays(3));
        when(tokens.marcarUsado(100L, AHORA)).thenReturn(false);

        assertThatThrownBy(() -> service.rotar("carrera")).isInstanceOf(CredencialesInvalidasException.class);

        verify(tokens).revocarFamilia("fam-5", AHORA);
        verify(tokens, never()).guardar(any());
    }

    // ---- cerrar, revocar y purgar ----

    @Test
    void cerrar_revocaLaFamiliaDelToken_yUnTokenDesconocidoNoEsError() {
        guardado("activo", "fam-2", AHORA.plusDays(3));
        when(tokens.porHash(TokenOpaco.hash("raro"))).thenReturn(Optional.empty());

        service.cerrar("activo");
        service.cerrar("raro");
        service.cerrar(null);

        verify(tokens).revocarFamilia("fam-2", AHORA);
        verify(tokens, org.mockito.Mockito.times(1)).revocarFamilia(any(), any());
    }

    @Test
    void revocarTodos_revocaLosTokensDeLaPersona() {
        service.revocarTodos(7L);

        verify(tokens).revocarDeUsuario(7L, AHORA);
    }

    @Test
    void purgarVencidos_borraLosVencidosHaceMasDeUnaSemana() {
        when(tokens.borrarExpiradosAntesDe(AHORA.minusDays(7))).thenReturn(12);

        assertThat(service.purgarVencidos()).isEqualTo(12);
    }

    @Test
    void rechazaUnaVigenciaNoPositiva() {
        assertThatThrownBy(() -> new RefrescoDeSesionService(tokens, 0, RELOJ))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @SuppressWarnings("unused")
    private void usos() {
        eq(1);
    }
}
