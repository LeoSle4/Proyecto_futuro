package com.kala.backend.identidad.infraestructura.web;

import com.kala.backend.identidad.aplicacion.ReporteAdminService.Reporte;
import com.kala.backend.identidad.aplicacion.ReporteAdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Reporte solo para ADMIN: la fecha del servidor y el total de usuarios por subtipo.
@RestController
@RequestMapping("/api/admin")
public class AdminReporteController {

    private final ReporteAdminService servicio;

    public AdminReporteController(ReporteAdminService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/reporte")
    @PreAuthorize("hasRole('ADMIN')")
    public ReporteAdminResponse reporte() {
        ReporteAdminService.Reporte r = servicio.reporte();
        return new ReporteAdminResponse(r.fecha(), r.usuarios(), r.empleados(), r.personal(), r.administradores());
    }
}
