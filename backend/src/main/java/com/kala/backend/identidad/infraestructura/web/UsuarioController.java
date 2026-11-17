package com.kala.backend.identidad.infraestructura.web;

import com.kala.backend.identidad.aplicacion.GestionUsuariosService;
import com.kala.backend.identidad.infraestructura.web.UsuarioDtos.CambioClaveRequest;
import com.kala.backend.identidad.infraestructura.web.UsuarioDtos.NuevoEmpleadoRequest;
import com.kala.backend.identidad.infraestructura.web.UsuarioDtos.NuevoPersonalRequest;
import com.kala.backend.identidad.infraestructura.web.UsuarioDtos.UsuarioCreadoResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Alta de cuentas y cambio de la propia clave. La empresa y la persona salen siempre del token.
@RestController
public class UsuarioController {

    private final GestionUsuariosService service;

    public UsuarioController(GestionUsuariosService service) {
        this.service = service;
    }

    // 201; 404 si la empresa no existe; 409 si el correo ya existe; 400 si el cuerpo no es válido.
    @PostMapping("/api/admin/empresas/{empresaId}/personal")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioCreadoResponse crearPersonal(@PathVariable Long empresaId,
                                               @Valid @RequestBody NuevoPersonalRequest request) {
        return UsuarioCreadoResponse.de(
                service.crearPersonal(empresaId, request.email(), request.nombre(), request.clave(), request.rol()));
    }

    // 201; 404 si el equipo no es de la empresa del token; 409 si el correo ya existe.
    @PostMapping("/api/empleados")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RRHH','COORDINADOR_BIENESTAR')")
    public UsuarioCreadoResponse crearEmpleado(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody NuevoEmpleadoRequest request) {
        return UsuarioCreadoResponse.de(service.crearEmpleado(
                claim(jwt, "empresaId"), request.equipoId(), request.email(), request.nombre(), request.clave()));
    }

    // 204; 401 si la clave actual no coincide; 400 si la nueva no cumple las reglas. Los tokens ya emitidos
    // siguen valiendo hasta que venzan.
    @PutMapping("/api/usuarios/me/clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void cambiarClave(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CambioClaveRequest request) {
        service.cambiarClave(claim(jwt, "uid"), request.claveActual(), request.claveNueva());
    }

    // 204; 404 si el usuario no existe o (para RRHH y coordinador) no es empleado de su empresa; 400 si es la propia cuenta.
    @PatchMapping("/api/usuarios/{id}/desactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','RRHH','COORDINADOR_BIENESTAR')")
    public void desactivar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        cambiarEstado(jwt, id, false);
    }

    @PatchMapping("/api/usuarios/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','RRHH','COORDINADOR_BIENESTAR')")
    public void activar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        cambiarEstado(jwt, id, true);
    }

    private void cambiarEstado(Jwt jwt, Long usuarioId, boolean activo) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        boolean esAdmin = roles != null && roles.contains("ROLE_ADMIN");
        Long empresaId = esAdmin ? null : claim(jwt, "empresaId");
        service.cambiarEstado(claim(jwt, "uid"), esAdmin, empresaId, usuarioId, activo);
    }

    private static Long claim(Jwt jwt, String nombre) {
        Object valor = jwt == null ? null : jwt.getClaim(nombre);
        if (valor instanceof Number numero) {
            return numero.longValue();
        }
        throw new AccessDeniedException("El token no incluye el claim " + nombre);
    }
}
