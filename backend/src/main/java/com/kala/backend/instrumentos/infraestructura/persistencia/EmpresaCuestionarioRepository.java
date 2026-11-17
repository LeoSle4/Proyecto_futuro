package com.kala.backend.instrumentos.infraestructura.persistencia;

import com.kala.backend.instrumentos.aplicacion.puerto.Activaciones;
import com.kala.backend.instrumentos.dominio.EmpresaCuestionario;
import com.kala.backend.instrumentos.dominio.EmpresaCuestionarioId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaCuestionarioRepository extends JpaRepository<EmpresaCuestionario, EmpresaCuestionarioId>, Activaciones {

    @Override
    default Optional<EmpresaCuestionario> porId(Long empresaId, Long cuestionarioId) {
        return findById(new EmpresaCuestionarioId(empresaId, cuestionarioId));
    }

    @Override
    default EmpresaCuestionario guardar(EmpresaCuestionario activacion) {
        return save(activacion);
    }
}
