package com.kala.backend.identidad.infraestructura.seguridad;

import com.kala.backend.identidad.aplicacion.LimitadorIntentosLogin;
import com.kala.backend.identidad.aplicacion.RefrescoDeSesionService.Sesion;
import com.kala.backend.identidad.aplicacion.RefrescoDeSesionService;
import com.kala.backend.identidad.aplicacion.puerto.Usuarios;
import com.kala.backend.identidad.dominio.CredencialesInvalidasException;
import com.kala.backend.identidad.dominio.Usuario;
import com.kala.backend.identidad.infraestructura.persistencia.IdentidadLector.Identidad;
import com.kala.backend.identidad.infraestructura.persistencia.IdentidadLector;
import com.kala.backend.identidad.infraestructura.web.LoginRequest;
import com.kala.backend.identidad.infraestructura.web.TokenResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

// Valida las credenciales con el AuthenticationManager (UserDetailsService + BCrypt) y emite el token.
@Service
public class AutenticacionService {

    private final AuthenticationManager manager;
    private final IdentidadLector identidades;
    private final TokenService tokens;
    private final LimitadorIntentosLogin limitador;
    private final RefrescoDeSesionService refresco;
    private final UserDetailsService detalles;
    private final Usuarios usuarios;

    public AutenticacionService(AuthenticationManager manager, IdentidadLector identidades, TokenService tokens,
                                LimitadorIntentosLogin limitador, RefrescoDeSesionService refresco,
                                UserDetailsService detalles, Usuarios usuarios) {
        this.manager = manager;
        this.identidades = identidades;
        this.tokens = tokens;
        this.limitador = limitador;
        this.refresco = refresco;
        this.detalles = detalles;
        this.usuarios = usuarios;
    }

    // 401 con credenciales inválidas; 429 si ese correo acumuló demasiados fallos (no se llega a calcular el BCrypt).
    public TokenResponse login(LoginRequest request) {
        limitador.verificar(request.username());
        Authentication autenticacion;
        try {
            autenticacion = manager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        } catch (AuthenticationException e) {
            limitador.registrarFallo(request.username());
            throw new CredencialesInvalidasException();
        }
        limitador.registrarExito(request.username());
        Identidad identidad = identidades.porEmail(autenticacion.getName());
        RefrescoDeSesionService.Sesion sesion = refresco.iniciar(identidad.uid());
        return new TokenResponse("Bearer", tokens.crearToken(autenticacion, identidad), tokens.expiraEnSegundos(),
                sesion.refreshToken(), sesion.expiraEnSegundos());
    }

    // Renueva la sesión: rota el refresh token y firma un JWT con la identidad ACTUAL (si cambiaron los roles, la
    // empresa o el equipo, el token nuevo ya lo refleja). Una cuenta desactivada o borrada pierde todas sus sesiones.
    // 401 si el refresh token no sirve.
    public TokenResponse refrescar(String refreshToken) {
        RefrescoDeSesionService.Sesion sesion = refresco.rotar(refreshToken);
        Usuario usuario = usuarios.porId(sesion.usuarioId()).orElse(null);
        UserDetails cuenta = usuario == null ? null : detalles.loadUserByUsername(usuario.getEmail());
        if (cuenta == null || !cuenta.isEnabled()) {
            refresco.revocarTodos(sesion.usuarioId());
            throw new CredencialesInvalidasException();
        }
        Authentication autenticacion = UsernamePasswordAuthenticationToken.authenticated(
                cuenta.getUsername(), null, cuenta.getAuthorities());
        Identidad identidad = identidades.porEmail(cuenta.getUsername());
        return new TokenResponse("Bearer", tokens.crearToken(autenticacion, identidad), tokens.expiraEnSegundos(),
                sesion.refreshToken(), sesion.expiraEnSegundos());
    }

    // Cierra la sesión: revoca la familia del refresh token. Un token desconocido no es un error.
    public void cerrarSesion(String refreshToken) {
        refresco.cerrar(refreshToken);
    }
}
