package com.kala.backend.instrumentos.infraestructura.persistencia;

import com.kala.backend.instrumentos.aplicacion.puerto.Presupuestos;
import com.kala.backend.instrumentos.dominio.PresupuestoPrivacidad;
import com.kala.backend.instrumentos.dominio.PresupuestoPrivacidadId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresupuestoPrivacidadRepository extends JpaRepository<PresupuestoPrivacidad, PresupuestoPrivacidadId>, Presupuestos {

    @Override
    default Optional<PresupuestoPrivacidad> deEmpresaYPeriodo(Long empresaId, String periodo) {
        return findById(new PresupuestoPrivacidadId(empresaId, periodo));
    }
}
