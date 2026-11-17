package com.kala.backend.identidad.infraestructura.seguridad;

import com.kala.backend.identidad.aplicacion.LimitadorIntentosLogin;
import com.kala.backend.identidad.dominio.CredencialesInvalidasException;
import com.kala.backend.identidad.infraestructura.persistencia.IdentidadLector.Identidad;
import com.kala.backend.identidad.infraestructura.persistencia.IdentidadLector;
import com.kala.backend.identidad.infraestructura.web.LoginRequest;
import com.kala.backend.identidad.infraestructura.web.TokenResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

// Valida las credenciales con el AuthenticationManager (UserDetailsService + BCrypt) y emite el token.
@Service
public class AutenticacionService {

    private final AuthenticationManager manager;
    private final IdentidadLector identidades;
    private final TokenService tokens;
    private final LimitadorIntentosLogin limitador;

    public AutenticacionService(AuthenticationManager manager, IdentidadLector identidades, TokenService tokens,
                                LimitadorIntentosLogin limitador) {
        this.manager = manager;
        this.identidades = identidades;
        this.tokens = tokens;
        this.limitador = limitador;
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
        return new TokenResponse("Bearer", tokens.crearToken(autenticacion, identidad), tokens.expiraEnSegundos());
    }
}
