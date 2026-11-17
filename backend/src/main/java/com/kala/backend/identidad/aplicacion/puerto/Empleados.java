package com.kala.backend.identidad.aplicacion.puerto;

import com.kala.backend.identidad.dominio.EmpleadoEquipo;
import java.util.Optional;

// Puerto de salida: el subtipo empleado (persona que responde check-ins).
public interface Empleados {

    EmpleadoEquipo guardar(EmpleadoEquipo empleado);

    Optional<EmpleadoEquipo> porUsuario(Long usuarioId);
}
