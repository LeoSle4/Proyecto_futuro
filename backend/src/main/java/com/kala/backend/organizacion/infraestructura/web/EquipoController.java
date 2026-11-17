package com.kala.backend.organizacion.infraestructura.web;

import com.kala.backend.organizacion.aplicacion.EquipoRequest;
import com.kala.backend.organizacion.aplicacion.EquipoService;
import com.kala.backend.organizacion.dominio.EmpresaNoEncontradaException;
import com.kala.backend.organizacion.dominio.Equipo;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Traduce las peticiones HTTP a llamadas del service. Sin lógica de negocio ni try/catch.
@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

    private final EquipoService service;

    public EquipoController(EquipoService service) {
        this.service = service;
    }

    // required = false -> el filtro por empresa es opcional.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RRHH', 'COORDINADOR_BIENESTAR')")
    public List<EquipoResponse> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long empresaId) {
        if (esAdmin(jwt)) {
            return service.listarPorEmpresa(empresaId).stream().map(EquipoResponse::de).toList();
        }
        return service.listarPorEmpresa(empresaId(jwt)).stream().map(EquipoResponse::de).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RRHH', 'COORDINADOR_BIENESTAR')")
    public EquipoResponse buscarPorId(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        if (!esAdmin(jwt)) {
            return EquipoResponse.de(service.buscarPorIdEnEmpresa(id, empresaId(jwt)));
        }
        return EquipoResponse.de(service.buscarPorId(id));
    }

    // 201 Created + header Location con la URL del recurso creado.
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RRHH')")
    public ResponseEntity<EquipoResponse> crear(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EquipoRequest request) {
        if (!esAdmin(jwt) && !empresaId(jwt).equals(request.empresaId())) {
            throw new EmpresaNoEncontradaException(request.empresaId());
        }
        Equipo creado = service.crear(request);
        return ResponseEntity.created(URI.create("/api/equipos/" + creado.getId())).body(EquipoResponse.de(creado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RRHH')")
    public EquipoResponse actualizar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody EquipoRequest request) {
        if (!esAdmin(jwt)) {
            return EquipoResponse.de(service.actualizarEnEmpresa(id, request, empresaId(jwt)));
        }
        return EquipoResponse.de(service.actualizar(id, request));
    }

    // 204 No Content: se eliminó y no hay cuerpo que devolver.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RRHH')")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        if (esAdmin(jwt)) {
            service.eliminar(id);
        } else {
            service.eliminarEnEmpresa(id, empresaId(jwt));
        }
        return ResponseEntity.noContent().build();
    }

    private static boolean esAdmin(Jwt jwt) {
        return jwt.getClaimAsStringList("roles").contains("ROLE_ADMIN");
    }

    private static Long empresaId(Jwt jwt) {
        Long empresaId = jwt.getClaim("empresaId");
        if (empresaId == null) {
            throw new AccessDeniedException("El token no contiene empresaId");
        }
        return empresaId;
    }
}
