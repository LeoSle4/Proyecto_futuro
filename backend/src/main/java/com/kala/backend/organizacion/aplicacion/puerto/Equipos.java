package com.kala.backend.organizacion.aplicacion.puerto;

import com.kala.backend.organizacion.dominio.Equipo;
import java.util.List;
import java.util.Optional;

// Puerto de salida: los equipos de una empresa.
public interface Equipos {

    List<Equipo> todos();

    List<Equipo> deEmpresa(Long empresaId);

    Optional<Equipo> porId(Long id);

    Optional<Equipo> porIdEnEmpresa(Long id, Long empresaId);

    boolean tieneEquipos(Long empresaId);

    Equipo guardar(Equipo equipo);

    void eliminar(Equipo equipo);
}
