package com.kala.backend.instrumentos.infraestructura.web;

import com.kala.backend.instrumentos.dominio.Cuestionario;

// Un cuestionario activo de la empresa, sin sus ítems.
public record CuestionarioResponse(
        Long id,
        String codigo,
        String nombre,
        int cadenciaDias,
        Integer itemsPorEnvio
) {
    public static CuestionarioResponse de(Cuestionario c) {
        return new CuestionarioResponse(c.getId(), c.getCodigo(), c.getNombre(), c.getCadenciaDias(),
                c.getItemsPorEnvio() == null ? null : c.getItemsPorEnvio().intValue());
    }
}
