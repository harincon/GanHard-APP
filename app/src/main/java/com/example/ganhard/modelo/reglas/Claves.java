package com.example.ganhard.modelo.reglas;

import java.text.Normalizer;
import java.util.Locale;

/** Comparar lo que la persona escribe: sin espacios sobrantes, sin tildes y sin distinguir mayúsculas. */
public final class Claves {
    private Claves() {}

    public static String de(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }
}
