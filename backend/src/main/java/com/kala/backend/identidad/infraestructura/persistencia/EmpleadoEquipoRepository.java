package com.kala.backend.identidad.infraestructura.persistencia;

import com.kala.backend.identidad.aplicacion.puerto.Empleados;
import com.kala.backend.identidad.dominio.EmpleadoEquipo;
import com.kala.backend.identidad.aplicacion.puerto.EmpleadoResumen;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoEquipoRepository extends JpaRepository<EmpleadoEquipo, Long>, Empleados {
    @Override
    default EmpleadoEquipo guardar(EmpleadoEquipo empleado) {
        return save(empleado);
    }

    @Query("""
            select new com.kala.backend.identidad.aplicacion.puerto.EmpleadoResumen(
                u.id, u.email, u.nombre, u.activo, q.id, q.nombre)
            from EmpleadoEquipo e, Usuario u, Equipo q
            where e.usuarioId = u.id and e.equipoId = q.id and q.empresaId = :empresaId
            order by u.nombre, u.id
            """)
    List<EmpleadoResumen> empleadosDeEmpresa(@Param("empresaId") Long empresaId);

    @Override
    default List<EmpleadoResumen> deEmpresa(Long empresaId) {
        return empleadosDeEmpresa(empresaId);
    }

    @Override
    default Optional<EmpleadoEquipo> porUsuario(Long usuarioId) {
        return findById(usuarioId);
    }
}
