package com.kala.backend.instrumentos.aplicacion.puerto;

import com.kala.backend.instrumentos.dominio.ConfiguracionLdp;
import java.util.Optional;

// Puerto de salida: las configuraciones LDP (mecanismo y parámetro).
public interface ConfiguracionesLdp {

    Optional<ConfiguracionLdp> porId(Long id);
}
