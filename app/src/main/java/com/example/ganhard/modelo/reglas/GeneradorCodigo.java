package com.example.ganhard.modelo.reglas;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Código automático del animal: GH-001, GH-002, … El siguiente es el mayor número ya usado más uno, así que un
 * número nunca se reutiliza (los animales vendidos o fallecidos siguen contando). Los códigos escritos antes con
 * otro formato se respetan y no entran en la cuenta.
 */
public final class GeneradorCodigo {
    public static final String PREFIJO = "GH-";
    private static final Pattern FORMATO = Pattern.compile("^GH-(\\d{1,9})$", Pattern.CASE_INSENSITIVE);

    private GeneradorCodigo() {}

    /** El código que sigue, dados todos los códigos que ya existen en la finca. */
    public static String siguiente(List<String> codigos) {
        long mayor = 0;
        for (String codigo : codigos) {
            if (codigo == null) continue;
            Matcher coincidencia = FORMATO.matcher(codigo.trim());
            if (coincidencia.matches()) mayor = Math.max(mayor, Long.parseLong(coincidencia.group(1)));
        }
        return String.format(Locale.ROOT, "%s%03d", PREFIJO, mayor + 1);
    }
}
