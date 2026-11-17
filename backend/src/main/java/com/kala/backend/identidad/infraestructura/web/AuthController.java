package com.kala.backend.identidad.infraestructura.web;

import com.kala.backend.identidad.infraestructura.seguridad.AutenticacionService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Traduce las peticiones HTTP a llamadas del service. Sin lógica de negocio ni try/catch.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AutenticacionService autenticacion;

    public AuthController(AutenticacionService autenticacion) {
        this.autenticacion = autenticacion;
    }

    // 200 con el token; 401 con credenciales inválidas; 400 si falta usuario o contraseña.
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return autenticacion.login(request);
    }

    // 200 con un JWT nuevo y el refresh token rotado; 401 si el refresh token no sirve (desconocido, vencido, ya usado
    // o revocado). Es público: la credencial es el propio refresh token.
    @PostMapping("/refresh")
    public TokenResponse refrescar(@Valid @RequestBody RefreshRequest request) {
        return autenticacion.refrescar(request.refreshToken());
    }

    // 204 siempre que el cuerpo sea válido: cerrar una sesión ya cerrada o desconocida no es un error.
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrarSesion(@Valid @RequestBody RefreshRequest request) {
        autenticacion.cerrarSesion(request.refreshToken());
    }

    // Devuelve lo que dice el token de quien consulta; no vuelve a validar credenciales.
    @GetMapping("/me")
    public Map<String, Object> miPerfil(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> perfil = new LinkedHashMap<>();
        perfil.put("username", jwt.getSubject());
        perfil.put("uid", jwt.getClaim("uid"));
        perfil.put("roles", jwt.getClaim("roles"));
        if (jwt.getClaim("empresaId") != null) {
            perfil.put("empresaId", jwt.getClaim("empresaId"));
        }
        if (jwt.getClaim("equipoId") != null) {
            perfil.put("equipoId", jwt.getClaim("equipoId"));
        }
        return perfil;
    }
}
