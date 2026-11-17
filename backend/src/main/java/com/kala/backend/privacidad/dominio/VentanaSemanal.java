package com.kala.backend.privacidad.dominio;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

// La ventana del antiduplicado es la semana que empieza el lunes. Se calcula en la zona horaria de la
// aplicación (America/Lima) para que el cambio de semana ocurra a la medianoche local.
public final class VentanaSemanal {

    // Lunes desde el que se cuentan los bloques de una cadencia. Con cadencia 7 los bloques son las semanas.
    private static final LocalDate ANCLA = LocalDate.of(2024, 1, 1);

    private VentanaSemanal() {
    }

    // Lunes de la semana a la que pertenece la fecha.
    public static LocalDate inicioDeVentana(LocalDate fecha) {
        return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    // Lunes de la semana local en la que cae el instante.
    public static LocalDate inicioDeVentana(Instant instante, ZoneId zona) {
        return inicioDeVentana(instante.atZone(zona).toLocalDate());
    }

    // Inicio del bloque de cadenciaDias días al que pertenece la fecha. Un cuestionario con cadencia de 14 días
    // solo se responde una vez por bloque, de modo que la cota de ε por persona al mes se cumple.
    public static LocalDate inicioDeVentana(LocalDate fecha, int cadenciaDias) {
        if (cadenciaDias <= 0) {
            throw new IllegalArgumentException("La cadencia debe ser positiva");
        }
        long bloque = Math.floorDiv(ChronoUnit.DAYS.between(ANCLA, fecha), (long) cadenciaDias);
        return ANCLA.plusDays(bloque * cadenciaDias);
    }

    public static LocalDate inicioDeVentana(Instant instante, ZoneId zona, int cadenciaDias) {
        return inicioDeVentana(instante.atZone(zona).toLocalDate(), cadenciaDias);
    }
}
