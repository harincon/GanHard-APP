package com.example.ganhard.modelo;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;

/** Preferencia de tema claro, oscuro o del sistema (RF18). Persiste en las preferencias privadas de la app. */
public final class PreferenciaTema {
    private static final String ARCHIVO = "preferencias";
    private static final String CLAVE = "tema";

    private PreferenciaTema() {}

    /** Modo guardado de {@link AppCompatDelegate}; sin preferencia, el del sistema. */
    public static int leer(Context contexto) {
        return contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).getInt(CLAVE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    /** Guarda el modo y lo aplica de inmediato. */
    public static void guardar(Context contexto, int modo) {
        contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit().putInt(CLAVE, modo).apply();
        AppCompatDelegate.setDefaultNightMode(modo);
    }

    /** Aplica el modo guardado; se llama al iniciar el proceso. */
    public static void aplicar(Context contexto) {
        AppCompatDelegate.setDefaultNightMode(leer(contexto));
    }
}
