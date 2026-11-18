package com.kala.backend.identidad.aplicacion.puerto;

import com.kala.backend.identidad.dominio.EmpleadoEquipo;
import java.util.List;
import java.util.Optional;

// Puerto de salida: el subtipo empleado (persona que responde check-ins).
public interface Empleados {

    EmpleadoEquipo guardar(EmpleadoEquipo empleado);

    Optional<EmpleadoEquipo> porUsuario(Long usuarioId);

    // Los empleados de los equipos de una empresa, ordenados por nombre.
    List<EmpleadoResumen> deEmpresa(Long empresaId);
}
