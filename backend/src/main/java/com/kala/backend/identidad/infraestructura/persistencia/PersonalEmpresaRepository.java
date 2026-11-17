package com.kala.backend.identidad.infraestructura.persistencia;

import com.kala.backend.identidad.dominio.PersonalEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalEmpresaRepository extends JpaRepository<PersonalEmpresa, Long> {
}
