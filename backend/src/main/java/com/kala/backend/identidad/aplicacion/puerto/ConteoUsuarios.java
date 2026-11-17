package com.kala.backend.identidad.aplicacion.puerto;

// Puerto de salida: cuántos usuarios hay por subtipo.
public interface ConteoUsuarios {

    long usuarios();

    long empleados();

    long personal();

    long administradores();
}
