package com.kala.backend.dashboard.aplicacion.puerto;

import com.kala.backend.dashboard.dominio.ConsultaDashboard;

// Puerto de salida: la auditoría de quién de RRHH consultó qué equipo y cuándo.
public interface AuditoriaDashboard {

    ConsultaDashboard guardar(ConsultaDashboard consulta);
}
