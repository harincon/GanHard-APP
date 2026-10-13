package com.example.ganhard.controlador;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formatos de fecha y hora para la pantalla. Los datos se guardan como AAAA-MM-DD y HH:MM:SS; aquí se
 * convierten a lo que muestra el diseño ("15 ene 2026", "8:15 a. m.", "05/03/2025") y de vuelta al ISO.
 * Todo lo que no se reconoce se devuelve tal cual, para que el validador del Modelo lo marque como formato.
 */
final class FormatoFecha {
    private static final String[] MESES = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"};
    private static final Pattern PANTALLA = Pattern.compile("^(\\d{1,2})/(\\d{1,2})/(\\d{4})$");
    private static final Pattern HORA = Pattern.compile("^(\\d{1,2}):(\\d{2})(?::\\d{2})?$");

    private FormatoFecha() {}

    /** "2026-01-15" → "15 ene 2026". */
    static String corta(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        LocalDate fecha = leerIso(iso);
        if (fecha == null) return iso;
        return fecha.getDayOfMonth() + " " + MESES[fecha.getMonthValue() - 1] + " " + fecha.getYear();
    }

    /** "08:15:00" → "8:15 a. m."; "00:05" → "12:05 a. m."; "12:00" → "12:00 p. m.". */
    static String hora12(String hora) {
        if (hora == null) return "";
        Matcher m = HORA.matcher(hora);
        if (!m.matches()) return hora;
        int h = Integer.parseInt(m.group(1));
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return String.format(Locale.US, "%d:%s %s", h12, m.group(2), h < 12 ? "a. m." : "p. m.");
    }

    /** "10 oct 2026 · 8:15 a. m."; sin hora usa el texto recibido (viene de recursos). */
    static String fechaYHora(String iso, String horaONull, String sinHora) {
        return corta(iso) + " · " + (horaONull == null || horaONull.isEmpty() ? sinHora : hora12(horaONull));
    }

    /** "2025-03-05" → "05/03/2025", para los campos de los formularios. */
    static String aPantalla(String iso) {
        LocalDate fecha = leerIso(iso);
        if (fecha == null) return iso == null ? "" : iso;
        return String.format(Locale.US, "%02d/%02d/%04d", fecha.getDayOfMonth(), fecha.getMonthValue(), fecha.getYear());
    }

    /** "05/03/2025" o "5/3/2025" → "2025-03-05"; lo que no sea una fecha válida se devuelve igual. */
    static String aIso(String texto) {
        if (texto == null) return "";
        Matcher m = PANTALLA.matcher(texto.trim());
        if (!m.matches()) return texto;
        try {
            return LocalDate.of(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1))).toString();
        } catch (DateTimeException ex) {
            return texto;
        }
    }

    /** "2026-10-10" + "08:15:00" → "10 oct 2026, 8:15 a. m."; sin hora, solo la fecha. */
    static String fechaYHoraConComa(String iso, String horaONull) {
        return horaONull == null || horaONull.isEmpty() ? corta(iso) : corta(iso) + ", " + hora12(horaONull);
    }

    private static LocalDate leerIso(String iso) {
        if (iso == null) return null;
        try { return LocalDate.parse(iso); }
        catch (DateTimeException ex) { return null; }
    }
}
