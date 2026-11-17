package com.kala.backend.identidad.infraestructura.persistencia;

import com.kala.backend.identidad.dominio.AdministradorPlataforma;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministradorPlataformaRepository extends JpaRepository<AdministradorPlataforma, Long> {
}
