package com.kala.backend.dashboard.infraestructura.persistencia;

import com.kala.backend.dashboard.aplicacion.puerto.AuditoriaDashboard;
import com.kala.backend.dashboard.dominio.ConsultaDashboard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultaDashboardRepository extends JpaRepository<ConsultaDashboard, Long>, AuditoriaDashboard {

    @Override
    default ConsultaDashboard guardar(ConsultaDashboard consulta) {
        return save(consulta);
    }
}
