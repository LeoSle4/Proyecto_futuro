package com.kala.backend.identidad.infraestructura.persistencia;

import com.kala.backend.identidad.dominio.EmpleadoEquipo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoEquipoRepository extends JpaRepository<EmpleadoEquipo, Long> {
}
