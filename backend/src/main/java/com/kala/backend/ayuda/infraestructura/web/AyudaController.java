package com.kala.backend.ayuda.infraestructura.web;

import com.kala.backend.ayuda.aplicacion.AyudaService;
import com.kala.backend.ayuda.infraestructura.web.AyudaDtos.AtenderRequest;
import com.kala.backend.ayuda.infraestructura.web.AyudaDtos.ContactoRequest;
import com.kala.backend.ayuda.infraestructura.web.AyudaDtos.ContactoResponse;
import com.kala.backend.ayuda.infraestructura.web.AyudaDtos.SolicitudRequest;
import com.kala.backend.ayuda.infraestructura.web.AyudaDtos.SolicitudResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Canal "Hablar con alguien". La empresa y la persona salen siempre del token, nunca de la URL ni del cuerpo.
// Las solicitudes las ve solo el coordinador de bienestar: RRHH no sabe quién pidió ayuda.
@RestController
@RequestMapping("/api/ayuda")
public class AyudaController {

    private static final String PERSONAL = "hasAnyRole('RRHH','COORDINADOR_BIENESTAR')";
    private static final String COORDINADOR = "hasRole('COORDINADOR_BIENESTAR')";

    private final AyudaService service;

    public AyudaController(AyudaService service) {
        this.service = service;
    }

    // El empleado ve solo los contactos activos; el personal ve todos para poder gestionarlos.
    @GetMapping("/contactos")
    @PreAuthorize("hasAnyRole('EMPLEADO','RRHH','COORDINADOR_BIENESTAR')")
    public List<ContactoResponse> contactos(@AuthenticationPrincipal Jwt jwt) {
        boolean esEmpleado = jwt.getClaimAsStringList("roles") != null
                && jwt.getClaimAsStringList("roles").contains("ROLE_EMPLEADO");
        return service.listarContactos(claim(jwt, "empresaId"), esEmpleado).stream()
                .map(ContactoResponse::de).toList();
    }

    @PostMapping("/contactos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(PERSONAL)
    public ContactoResponse crearContacto(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ContactoRequest request) {
        return ContactoResponse.de(service.crearContacto(claim(jwt, "empresaId"),
                request.nombre(), request.tipo(), request.telefono(), request.email()));
    }

    @PatchMapping("/contactos/{id}/desactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(PERSONAL)
    public void desactivarContacto(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.desactivarContacto(claim(jwt, "empresaId"), id);
    }

    @PostMapping("/solicitudes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('EMPLEADO')")
    public SolicitudResponse solicitar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SolicitudRequest request) {
        return SolicitudResponse.de(service.solicitar(claim(jwt, "uid"), request.mensaje()));
    }

    @GetMapping("/solicitudes/mias")
    @PreAuthorize("hasRole('EMPLEADO')")
    public List<SolicitudResponse> mias(@AuthenticationPrincipal Jwt jwt) {
        return service.misSolicitudes(claim(jwt, "uid")).stream().map(SolicitudResponse::de).toList();
    }

    @GetMapping("/solicitudes")
    @PreAuthorize(COORDINADOR)
    public List<SolicitudResponse> solicitudes(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam(required = false) String estado) {
        return service.solicitudesDeEmpresa(claim(jwt, "empresaId"), estado).stream()
                .map(SolicitudResponse::de).toList();
    }

    // 200; 404 si la solicitud o el contacto no son de la empresa; 409 si la solicitud ya no está pendiente.
    @PatchMapping("/solicitudes/{id}/atender")
    @PreAuthorize(COORDINADOR)
    public SolicitudResponse atender(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                     @RequestBody(required = false) AtenderRequest request) {
        Long contactoId = request == null ? null : request.contactoId();
        return SolicitudResponse.de(service.atender(claim(jwt, "empresaId"), claim(jwt, "uid"), id, contactoId));
    }

    @PatchMapping("/solicitudes/{id}/cerrar")
    @PreAuthorize(COORDINADOR)
    public SolicitudResponse cerrar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return SolicitudResponse.de(service.cerrar(claim(jwt, "empresaId"), claim(jwt, "uid"), id));
    }

    // Sin el claim no se puede saber a qué empresa ni a qué persona corresponde la operación: 403.
    private static Long claim(Jwt jwt, String nombre) {
        Object valor = jwt == null ? null : jwt.getClaim(nombre);
        if (valor instanceof Number numero) {
            return numero.longValue();
        }
        throw new AccessDeniedException("El token no incluye el claim " + nombre);
    }
}
