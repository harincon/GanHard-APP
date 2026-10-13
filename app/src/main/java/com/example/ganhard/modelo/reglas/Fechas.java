package com.example.ganhard.modelo.reglas;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/** Lectura estricta de fechas (yyyy-MM-dd) y horas (HH:mm o HH:mm:ss); devuelve null si el texto no es válido. */
final class Fechas {
    private Fechas() {}

    static LocalDate fecha(String texto) {
        if (texto == null) return null;
        try { return LocalDate.parse(texto); }
        catch (DateTimeParseException ex) { return null; }
    }

    static LocalTime hora(String texto) {
        if (texto == null) return null;
        try { return LocalTime.parse(texto); }
        catch (DateTimeParseException ex) { return null; }
    }
}
