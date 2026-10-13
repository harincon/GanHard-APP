package com.example.ganhard.modelo.nucleo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Fechas y horas del Modelo. Los instantes se guardan en UTC; los hechos del usuario, en su fecha local. */
public final class Tiempo {
    private static final DateTimeFormatter INSTANTE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private Tiempo() {}

    public static String ahoraUtc() { return ahoraUtc(Clock.systemUTC()); }
    public static String ahoraUtc(Clock reloj) { return INSTANTE.format(reloj.instant()); }

    public static LocalDate hoy() { return hoy(Clock.systemDefaultZone()); }
    public static LocalDate hoy(Clock reloj) { return LocalDate.now(reloj); }

    public static LocalDateTime ahora() { return ahora(Clock.systemDefaultZone()); }
    public static LocalDateTime ahora(Clock reloj) { return LocalDateTime.now(reloj); }

    /** El selector de fechas de Material entrega la medianoche UTC del día elegido; no se convierte a zona local. */
    public static LocalDate fechaDeSelector(long milisUtc) {
        return Instant.ofEpochMilli(milisUtc).atZone(ZoneOffset.UTC).toLocalDate();
    }
}
