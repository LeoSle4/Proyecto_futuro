package com.kala.backend.identidad.infraestructura.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kala.backend.identidad.aplicacion.LimitadorIntentosLogin;
import com.kala.backend.identidad.aplicacion.RefrescoDeSesionService.Sesion;
import com.kala.backend.identidad.aplicacion.RefrescoDeSesionService;
import com.kala.backend.identidad.aplicacion.puerto.Usuarios;
import com.kala.backend.identidad.dominio.CredencialesInvalidasException;
import com.kala.backend.identidad.dominio.DemasiadosIntentosException;
import com.kala.backend.identidad.dominio.Usuario;
import com.kala.backend.identidad.infraestructura.persistencia.IdentidadLector.Identidad;
import com.kala.backend.identidad.infraestructura.persistencia.IdentidadLector;
import com.kala.backend.identidad.infraestructura.web.LoginRequest;
import com.kala.backend.identidad.infraestructura.web.TokenResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

class AutenticacionServiceTest {

    private AuthenticationManager manager;
    private IdentidadLector identidades;
    private TokenService tokens;
    private LimitadorIntentosLogin limitador;
    private RefrescoDeSesionService refresco;
    private UserDetailsService detalles;
    private Usuarios usuarios;
    private AutenticacionService service;

    @BeforeEach
    void setUp() {
        manager = mock(AuthenticationManager.class);
        identidades = mock(IdentidadLector.class);
        tokens = mock(TokenService.class);
        limitador = new LimitadorIntentosLogin(3, java.time.Duration.ofMinutes(15), java.time.Duration.ofMinutes(15),
                java.time.Clock.systemUTC());
        refresco = mock(RefrescoDeSesionService.class);
        detalles = mock(UserDetailsService.class);
        usuarios = mock(Usuarios.class);
        when(refresco.iniciar(any())).thenReturn(new RefrescoDeSesionService.Sesion(7L, "refresh-1", 2_592_000L));
        service = new AutenticacionService(manager, identidades, tokens, limitador, refresco, detalles, usuarios);
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

        assertThat(respuesta).isEqualTo(new TokenResponse("Bearer", "token-firmado", 1800L, "refresh-1", 2_592_000L));
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

    // ---- refresh ----

    private Usuario ana() {
        Usuario u = new Usuario("ana@kala.test", "hash", "Ana");
        org.springframework.test.util.ReflectionTestUtils.setField(u, "id", 7L);
        return u;
    }

    private UserDetails detallesDe(String email, boolean habilitado, String... roles) {
        return User.withUsername(email).password("x").authorities(roles).disabled(!habilitado).build();
    }

    @Test
    void refrescar_rotaElTokenYArmaUnJwtConLaIdentidadActual() {
        when(refresco.rotar("viejo")).thenReturn(new RefrescoDeSesionService.Sesion(7L, "nuevo", 2_592_000L));
        when(usuarios.porId(7L)).thenReturn(Optional.of(ana()));
        when(detalles.loadUserByUsername("ana@kala.test")).thenReturn(detallesDe("ana@kala.test", true, "ROLE_RRHH"));
        Identidad actual = new Identidad(7L, 9L, null);
        when(identidades.porEmail("ana@kala.test")).thenReturn(actual);
        when(tokens.crearToken(any(), org.mockito.ArgumentMatchers.eq(actual))).thenReturn("jwt-nuevo");
        when(tokens.expiraEnSegundos()).thenReturn(1800L);

        TokenResponse r = service.refrescar("viejo");

        assertThat(r).isEqualTo(new TokenResponse("Bearer", "jwt-nuevo", 1800L, "nuevo", 2_592_000L));
        org.mockito.ArgumentCaptor<Authentication> captor = org.mockito.ArgumentCaptor.forClass(Authentication.class);
        org.mockito.Mockito.verify(tokens).crearToken(captor.capture(), any());
        assertThat(captor.getValue().getAuthorities()).extracting(Object::toString).containsExactly("ROLE_RRHH");
    }

    @Test
    void refrescar_conUnTokenInvalido_propagaElErrorYNoEmiteNada() {
        when(refresco.rotar("malo")).thenThrow(new CredencialesInvalidasException());

        assertThatThrownBy(() -> service.refrescar("malo")).isInstanceOf(CredencialesInvalidasException.class);
        org.mockito.Mockito.verifyNoInteractions(tokens, detalles);
    }

    @Test
    void refrescar_deUnaCuentaDesactivada_revocaTodasSusSesiones_yEs401() {
        when(refresco.rotar("viejo")).thenReturn(new RefrescoDeSesionService.Sesion(7L, "nuevo", 2_592_000L));
        when(usuarios.porId(7L)).thenReturn(Optional.of(ana()));
        when(detalles.loadUserByUsername("ana@kala.test")).thenReturn(detallesDe("ana@kala.test", false, "ROLE_RRHH"));

        assertThatThrownBy(() -> service.refrescar("viejo")).isInstanceOf(CredencialesInvalidasException.class);

        org.mockito.Mockito.verify(refresco).revocarTodos(7L);
        org.mockito.Mockito.verify(tokens, org.mockito.Mockito.never()).crearToken(any(), any());
    }

    @Test
    void refrescar_deUnUsuarioQueYaNoExiste_revocaSusSesiones_yEs401() {
        when(refresco.rotar("viejo")).thenReturn(new RefrescoDeSesionService.Sesion(7L, "nuevo", 2_592_000L));
        when(usuarios.porId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refrescar("viejo")).isInstanceOf(CredencialesInvalidasException.class);

        org.mockito.Mockito.verify(refresco).revocarTodos(7L);
    }

    @Test
    void cerrarSesion_revocaLaFamiliaDelToken() {
        service.cerrarSesion("activo");

        org.mockito.Mockito.verify(refresco).cerrar("activo");
    }
}
