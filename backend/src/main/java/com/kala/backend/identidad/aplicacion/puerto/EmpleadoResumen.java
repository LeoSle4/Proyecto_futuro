package com.kala.backend.identidad.aplicacion.puerto;

// Lo que el panel ve de un empleado: quién es, en qué equipo está y si su cuenta está activa. Nunca la clave.
public record EmpleadoResumen(Long usuarioId, String email, String nombre, boolean activo, Long equipoId,
                              String equipoNombre) {
}
