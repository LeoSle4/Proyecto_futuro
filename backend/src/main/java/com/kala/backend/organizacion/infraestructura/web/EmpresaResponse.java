package com.kala.backend.organizacion.infraestructura.web;

import com.kala.backend.organizacion.dominio.Empresa;
import java.time.LocalDateTime;

// Lo que devuelve la API de empresas. Mantiene los campos de siempre sin exponer la entidad de la base de datos.
public record EmpresaResponse(Long id, String ruc, String nombre, String rubro, LocalDateTime creadoEn) {

    public static EmpresaResponse de(Empresa empresa) {
        return new EmpresaResponse(empresa.getId(), empresa.getRuc(), empresa.getNombre(), empresa.getRubro(),
                empresa.getCreadoEn());
    }
}
