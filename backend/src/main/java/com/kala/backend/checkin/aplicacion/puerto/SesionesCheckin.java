package com.kala.backend.checkin.aplicacion.puerto;

import java.time.LocalDate;

// Puerto de salida: el antiduplicado. Solo guarda un hash, el cuestionario y la fecha de la ventana (R4, R5, R6).
public interface SesionesCheckin {

    // Atómico y sin carreras: devuelve 1 si insertó y 0 si el hash ya existía (envío duplicado).
    int insertarSiNoExiste(String hash, Long cuestionarioId, LocalDate ventanaInicio);
}
