package com.kala.backend.instrumentos.infraestructura.web;

import com.kala.backend.instrumentos.aplicacion.ActivacionCuestionarioService.ResultadoActivacion;
import com.kala.backend.instrumentos.aplicacion.ActivacionCuestionarioService.ResultadoPresupuesto;
import com.kala.backend.instrumentos.aplicacion.ActivacionCuestionarioService;
import com.kala.backend.instrumentos.aplicacion.CuestionarioConsultaService;
import com.kala.backend.instrumentos.aplicacion.CuestionarioPendienteResponse;
import com.kala.backend.instrumentos.infraestructura.web.PresupuestoDtos.PresupuestoRequest;
import com.kala.backend.instrumentos.infraestructura.web.PresupuestoDtos.PresupuestoResponse;
import com.kala.backend.organizacion.dominio.EmpresaNoEncontradaException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Traduce HTTP a llamadas de service. Quién puede hacer qué se decide con @PreAuthorize y con el claim empresaId.
@RestController
public class CuestionarioController {

    private final ActivacionCuestionarioService activacion;
    private final CuestionarioConsultaService consulta;

    public CuestionarioController(ActivacionCuestionarioService activacion, CuestionarioConsultaService consulta) {
        this.activacion = activacion;
        this.consulta = consulta;
    }

    // 200; 404 si la empresa o el cuestionario no existen; 422 si excede el presupuesto de privacidad.
    @PostMapping("/api/empresas/{empresaId}/cuestionarios/{cuestionarioId}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ActivacionResponse activar(@PathVariable Long empresaId, @PathVariable Long cuestionarioId) {
        ResultadoActivacion r = activacion.activar(empresaId, cuestionarioId);
        return new ActivacionResponse(r.empresaId(), r.cuestionarioId(), r.epsilonMensual(), r.epsilonMaximo());
    }

    // 200 con el gasto que queda; idempotente; 404 si la empresa o el cuestionario no existen.
    @PostMapping("/api/empresas/{empresaId}/cuestionarios/{cuestionarioId}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ActivacionResponse desactivar(@PathVariable Long empresaId, @PathVariable Long cuestionarioId) {
        ResultadoActivacion r = activacion.desactivar(empresaId, cuestionarioId);
        return new ActivacionResponse(r.empresaId(), r.cuestionarioId(), r.epsilonMensual(), r.epsilonMaximo());
    }

    // ADMIN ve cualquier empresa; RRHH y COORDINADOR_BIENESTAR solo la suya (otra empresa responde 404).
    @GetMapping("/api/empresas/{empresaId}/presupuesto")
    @PreAuthorize("hasAnyRole('ADMIN', 'RRHH', 'COORDINADOR_BIENESTAR')")
    public PresupuestoResponse presupuesto(@PathVariable Long empresaId, @AuthenticationPrincipal Jwt jwt) {
        if (!esAdmin(jwt) && !empresaId.equals(empresaId(jwt))) {
            throw new EmpresaNoEncontradaException(empresaId);
        }
        return aRespuesta(activacion.consultarPresupuesto(empresaId));
    }

    // 200; 400 si el valor no está entre 0 (excluido) y 999.999; 404 si la empresa no existe;
    // 422 si es menor que lo que ya gastan los cuestionarios activos.
    @PutMapping("/api/empresas/{empresaId}/presupuesto")
    @PreAuthorize("hasRole('ADMIN')")
    public PresupuestoResponse fijarPresupuesto(@PathVariable Long empresaId, @Valid @RequestBody PresupuestoRequest request) {
        return aRespuesta(activacion.fijarPresupuesto(empresaId, request.epsilonMaximoPersona()));
    }

    private static PresupuestoResponse aRespuesta(ActivacionCuestionarioService.ResultadoPresupuesto r) {
        return new PresupuestoResponse(r.empresaId(), r.periodo(), r.epsilonMaximo(), r.epsilonMensual());
    }

    // ADMIN ve cualquier empresa; RRHH y COORDINADOR_BIENESTAR solo la suya (otra empresa responde 404).
    @GetMapping("/api/empresas/{empresaId}/cuestionarios")
    @PreAuthorize("hasAnyRole('ADMIN', 'RRHH', 'COORDINADOR_BIENESTAR')")
    public List<CuestionarioResponse> listar(@PathVariable Long empresaId, @AuthenticationPrincipal Jwt jwt,
                                             @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        if (!esAdmin(jwt) && !empresaId.equals(empresaId(jwt))) {
            throw new EmpresaNoEncontradaException(empresaId);
        }
        if (incluirInactivos) {
            return consulta.listarConEstado(empresaId).stream()
                    .map(c -> CuestionarioResponse.de(c.cuestionario(), c.activo())).toList();
        }
        return consulta.listarActivos(empresaId).stream().map(CuestionarioResponse::de).toList();
    }

    // El empleado recibe los cuestionarios activos de su empresa (la del token) para aleatorizar en su dispositivo.
    @GetMapping("/api/cuestionarios/pendientes")
    @PreAuthorize("hasRole('EMPLEADO')")
    public List<CuestionarioPendienteResponse> pendientes(@AuthenticationPrincipal Jwt jwt) {
        return consulta.pendientes(empresaId(jwt));
    }

    private static boolean esAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("ROLE_ADMIN");
    }

    private static Long empresaId(Jwt jwt) {
        Long empresaId = jwt.getClaim("empresaId");
        if (empresaId == null) {
            throw new AccessDeniedException("El token no contiene empresaId");
        }
        return empresaId;
    }
}
