package com.kala.backend.identidad.infraestructura.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kala.backend.identidad.aplicacion.LimitadorIntentosLogin;
import com.kala.backend.identidad.dominio.CredencialesInvalidasException;
import com.kala.backend.identidad.dominio.DemasiadosIntentosException;
import com.kala.backend.identidad.infraestructura.persistencia.IdentidadLector.Identidad;
import com.kala.backend.identidad.infraestructura.persistencia.IdentidadLector;
import com.kala.backend.identidad.infraestructura.web.LoginRequest;
import com.kala.backend.identidad.infraestructura.web.TokenResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class AutenticacionServiceTest {

    private AuthenticationManager manager;
    private IdentidadLector identidades;
    private TokenService tokens;
    private LimitadorIntentosLogin limitador;
    private AutenticacionService service;

    @BeforeEach
    void setUp() {
        manager = mock(AuthenticationManager.class);
        identidades = mock(IdentidadLector.class);
        tokens = mock(TokenService.class);
        limitador = new LimitadorIntentosLogin(3, java.time.Duration.ofMinutes(15), java.time.Duration.ofMinutes(15),
                java.time.Clock.systemUTC());
        service = new AutenticacionService(manager, identidades, tokens, limitador);
    }

    @Test
    void credencialesValidas_devuelvenElTokenBearerConSuVigencia() {
        Authentication autenticado = UsernamePasswordAuthenticationToken.authenticated(
                "ana@kala.test", null, List.of(new SimpleGrantedAuthority("ROLE_RRHH")));
        Identidad identidad = new Identidad(7L, 9L, null);
        when(manager.authenticate(any())).thenReturn(autenticado);
        when(identidades.porEmail("ana@kala.test")).thenReturn(identidad);
        when(tokens.crearToken(autenticado, identidad)).thenReturn("token-firmado");
        when(tokens.expiraEnSegundos()).thenReturn(1800L);

        TokenResponse respuesta = service.login(new LoginRequest("Ana@kala.test", "clave-segura-123"));

        assertThat(respuesta).isEqualTo(new TokenResponse("Bearer", "token-firmado", 1800L));
    }

    @Test
    void contrasenaIncorrecta_lanzaCredencialesInvalidas_sinEmitirToken() {
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("mal"));

        assertThatThrownBy(() -> service.login(new LoginRequest("ana@kala.test", "otra")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Credenciales inválidas");

        verifyNoInteractions(tokens, identidades);
    }

    @Test
    void cuentaDeshabilitada_seResponde_igualQueUnaContrasenaIncorrecta() {
        when(manager.authenticate(any())).thenThrow(new DisabledException("deshabilitada"));

        assertThatThrownBy(() -> service.login(new LoginRequest("ana@kala.test", "clave-segura-123")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    void tras_losFallosPermitidos_bloqueaSinPedirleNadaAlAuthenticationManager() {
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("mal"));
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.login(new LoginRequest("ANA@kala.test", "otra")))
                    .isInstanceOf(CredencialesInvalidasException.class);
        }

        assertThatThrownBy(() -> service.login(new LoginRequest("ana@kala.test", "clave-segura-123")))
                .isInstanceOf(DemasiadosIntentosException.class);
        org.mockito.Mockito.verify(manager, org.mockito.Mockito.times(3)).authenticate(any());
    }

    @Test
    void unInicioDeSesionExitoso_reiniciaLaCuentaDeFallos() {
        Authentication autenticado = UsernamePasswordAuthenticationToken.authenticated(
                "ana@kala.test", null, List.of(new SimpleGrantedAuthority("ROLE_RRHH")));
        when(manager.authenticate(any()))
                .thenThrow(new BadCredentialsException("mal"))
                .thenThrow(new BadCredentialsException("mal"))
                .thenReturn(autenticado)
                .thenThrow(new BadCredentialsException("mal"))
                .thenThrow(new BadCredentialsException("mal"))
                .thenReturn(autenticado);
        when(identidades.porEmail("ana@kala.test")).thenReturn(new Identidad(7L, 9L, null));
        when(tokens.crearToken(any(), any())).thenReturn("t");
        for (int ronda = 0; ronda < 2; ronda++) {
            for (int i = 0; i < 2; i++) {
                assertThatThrownBy(() -> service.login(new LoginRequest("ana@kala.test", "mal")))
                        .isInstanceOf(CredencialesInvalidasException.class);
            }
            assertThat(service.login(new LoginRequest("ana@kala.test", "bien")).accessToken()).isEqualTo("t");
        }
    }
}
