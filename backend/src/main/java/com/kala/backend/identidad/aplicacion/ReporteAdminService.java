package com.kala.backend.identidad.aplicacion;

import com.kala.backend.identidad.aplicacion.puerto.ConteoUsuarios;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Reporte solo para ADMIN: la fecha del servidor y el total de usuarios por subtipo.
@Service
public class ReporteAdminService {

    public record Reporte(LocalDate fecha, long usuarios, long empleados, long personal, long administradores) {
    }

    private final ConteoUsuarios conteos;
    private final ZoneId zona;

    public ReporteAdminService(ConteoUsuarios conteos, @Value("${kala.ventana.zona}") String zona) {
        this.conteos = conteos;
        this.zona = ZoneId.of(zona);
    }

    public Reporte reporte() {
        return new Reporte(LocalDate.now(zona), conteos.usuarios(), conteos.empleados(), conteos.personal(),
                conteos.administradores());
    }
}
