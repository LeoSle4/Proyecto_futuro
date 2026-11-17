package com.kala.backend.organizacion.infraestructura.web;

import com.kala.backend.organizacion.dominio.Equipo;
import java.time.LocalDateTime;

// Lo que devuelve la API de equipos. Mantiene los campos de siempre sin exponer la entidad de la base de datos.
public record EquipoResponse(Long id, String nombre, Long empresaId, LocalDateTime creadoEn) {

    public static EquipoResponse de(Equipo equipo) {
        return new EquipoResponse(equipo.getId(), equipo.getNombre(), equipo.getEmpresaId(), equipo.getCreadoEn());
    }
}
