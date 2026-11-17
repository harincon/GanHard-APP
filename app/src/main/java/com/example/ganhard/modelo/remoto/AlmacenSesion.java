package com.example.ganhard.modelo.remoto;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Guarda la sesión en el teléfono para no pedir la contraseña cada vez. Vive en las preferencias privadas de la app,
 * que quedan fuera de las copias de seguridad (ver backup_rules.xml); nunca se guarda la contraseña.
 */
@SuppressLint("ApplySharedPref") // commit() a propósito: la sesión debe quedar guardada antes de seguir, y se escribe fuera del hilo principal.
public final class AlmacenSesion {
    private static final String ARCHIVO = "sesion_remota";
    private static final String ACCESO = "acceso", RENOVACION = "renovacion", CUENTA = "cuenta", CORREO = "correo",
            CONFIRMADO = "confirmado", EXPIRA = "expira";

    private final SharedPreferences preferencias;

    public AlmacenSesion(Context contexto) {
        preferencias = contexto.getApplicationContext().getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    /** La sesión guardada, o nulo si no hay ninguna. */
    public synchronized SesionRemota leer() {
        String acceso = preferencias.getString(ACCESO, null), renovacion = preferencias.getString(RENOVACION, null);
        String cuenta = preferencias.getString(CUENTA, null);
        if (acceso == null || renovacion == null || cuenta == null) return null;
        return new SesionRemota(acceso, renovacion, cuenta, preferencias.getString(CORREO, ""),
                preferencias.getBoolean(CONFIRMADO, false), preferencias.getLong(EXPIRA, 0L));
    }

    public synchronized void guardar(SesionRemota sesion) {
        preferencias.edit().putString(ACCESO, sesion.accesoToken).putString(RENOVACION, sesion.renovacionToken)
                .putString(CUENTA, sesion.idCuenta).putString(CORREO, sesion.correo)
                .putBoolean(CONFIRMADO, sesion.correoConfirmado).putLong(EXPIRA, sesion.expiraEnSegundos).commit();
    }

    public synchronized void borrar() { preferencias.edit().clear().commit(); }
}
