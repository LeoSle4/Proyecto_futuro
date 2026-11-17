package com.kala.backend.identidad.infraestructura.persistencia;

import com.kala.backend.identidad.aplicacion.puerto.Empleados;
import com.kala.backend.identidad.dominio.EmpleadoEquipo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoEquipoRepository extends JpaRepository<EmpleadoEquipo, Long>, Empleados {
    @Override
    default EmpleadoEquipo guardar(EmpleadoEquipo empleado) {
        return save(empleado);
    }

    @Override
    default Optional<EmpleadoEquipo> porUsuario(Long usuarioId) {
        return findById(usuarioId);
    }
}
