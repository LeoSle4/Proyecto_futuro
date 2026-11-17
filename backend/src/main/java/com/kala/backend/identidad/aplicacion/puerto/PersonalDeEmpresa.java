package com.kala.backend.identidad.aplicacion.puerto;

import com.kala.backend.identidad.dominio.PersonalEmpresa;

// Puerto de salida: el subtipo personal de empresa (RRHH o coordinador de bienestar).
public interface PersonalDeEmpresa {

    PersonalEmpresa guardar(PersonalEmpresa personal);
}
