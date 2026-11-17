package com.kala.backend.organizacion.infraestructura.web;

import com.kala.backend.organizacion.aplicacion.EmpresaConEquiposRequest;
import com.kala.backend.organizacion.aplicacion.EmpresaRequest;
import com.kala.backend.organizacion.aplicacion.EmpresaService;
import com.kala.backend.organizacion.dominio.Empresa;
import com.kala.backend.organizacion.dominio.EmpresaNoEncontradaException;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaService service;

    public EmpresaController(EmpresaService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RRHH', 'COORDINADOR_BIENESTAR')")
    public List<EmpresaResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        if (esAdmin(jwt)) {
            return service.listar().stream().map(EmpresaResponse::de).toList();
        }
        return List.of(EmpresaResponse.de(service.buscarPorId(empresaId(jwt))));
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN', 'RRHH', 'COORDINADOR_BIENESTAR')")
    public List<EmpresaResponse> buscarPorNombre(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("texto") String texto) {

        if (texto == null || texto.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El texto de busqueda no puede estar vacio"
            );
        }

        if (esAdmin(jwt)) {
            return service.buscarPorNombre(texto).stream().map(EmpresaResponse::de).toList();
        }
        return service.buscarPorNombre(texto, empresaId(jwt)).stream().map(EmpresaResponse::de).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RRHH', 'COORDINADOR_BIENESTAR')")
    public EmpresaResponse buscarPorId(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        if (!esAdmin(jwt) && !id.equals(empresaId(jwt))) {
            throw new EmpresaNoEncontradaException(id);
        }
        return EmpresaResponse.de(service.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmpresaResponse> crear(
            @Valid @RequestBody EmpresaRequest request) {

        Empresa creada = service.crear(request);

        return ResponseEntity
                .created(URI.create("/api/empresas/" + creada.getId()))
                .body(EmpresaResponse.de(creada));
    }

    @PostMapping("/con-equipos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmpresaResponse> crearConEquipos(
            @Valid @RequestBody EmpresaConEquiposRequest request) {

        Empresa creada = service.crearConEquipos(request);

        return ResponseEntity
                .created(URI.create("/api/empresas/" + creada.getId()))
                .body(EmpresaResponse.de(creada));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EmpresaResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EmpresaRequest request) {

        return EmpresaResponse.de(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
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