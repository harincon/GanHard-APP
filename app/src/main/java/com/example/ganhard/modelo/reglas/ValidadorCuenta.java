package com.example.ganhard.modelo.reglas;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reglas del formulario de cuenta, aplicadas antes de llamar al servidor. El servidor vuelve a validar: estas reglas
 * solo evitan viajes inútiles y dan el mensaje junto al campo.
 */
public final class ValidadorCuenta {
    public static final int NOMBRE_MAXIMO = 100, CORREO_MAXIMO = 150, CONTRASENA_MINIMA = 8, CONTRASENA_MAXIMA = 72;

    private ValidadorCuenta() {}

    public static Map<String, ErrorCampo> registro(String nombre, String correo, String contrasena, String repetida) {
        Map<String, ErrorCampo> errores = new LinkedHashMap<>();
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.isEmpty()) errores.put(Campos.NOMBRE, new ErrorCampo(CodigoError.OBLIGATORIO));
        else if (limpio.length() > NOMBRE_MAXIMO) errores.put(Campos.NOMBRE, new ErrorCampo(CodigoError.LONGITUD_MAXIMA, NOMBRE_MAXIMO));
        ponerCorreo(errores, correo);
        String clave = contrasena == null ? "" : contrasena;
        if (clave.isEmpty()) errores.put(Campos.CONTRASENA, new ErrorCampo(CodigoError.OBLIGATORIO));
        else if (clave.length() < CONTRASENA_MINIMA) errores.put(Campos.CONTRASENA, new ErrorCampo(CodigoError.LONGITUD_MINIMA, CONTRASENA_MINIMA));
        // El servicio de cuentas limita la contraseña a 72 bytes, no a 72 letras.
        else if (clave.getBytes(StandardCharsets.UTF_8).length > CONTRASENA_MAXIMA) errores.put(Campos.CONTRASENA, new ErrorCampo(CodigoError.LONGITUD_MAXIMA, CONTRASENA_MAXIMA));
        else if (!clave.equals(repetida)) errores.put(Campos.CONTRASENA_REPETIDA, new ErrorCampo(CodigoError.NO_COINCIDE));
        return errores;
    }

    public static Map<String, ErrorCampo> inicio(String correo, String contrasena) {
        Map<String, ErrorCampo> errores = new LinkedHashMap<>();
        ponerCorreo(errores, correo);
        if (contrasena == null || contrasena.isEmpty()) errores.put(Campos.CONTRASENA, new ErrorCampo(CodigoError.OBLIGATORIO));
        return errores;
    }

    /** Nulo si el correo es válido. Sirve para recuperar el acceso y para invitar. */
    public static ErrorCampo correo(String correo) {
        String limpio = normalizarCorreo(correo);
        if (limpio.isEmpty()) return new ErrorCampo(CodigoError.OBLIGATORIO);
        if (limpio.length() > CORREO_MAXIMO) return new ErrorCampo(CodigoError.LONGITUD_MAXIMA, CORREO_MAXIMO);
        if (!limpio.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) return new ErrorCampo(CodigoError.FORMATO);
        return null;
    }

    /** Sin espacios en los extremos y en minúsculas, como lo guarda y compara el servidor. */
    public static String normalizarCorreo(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    private static void ponerCorreo(Map<String, ErrorCampo> errores, String correo) {
        ErrorCampo error = correo(correo);
        if (error != null) errores.put(Campos.CORREO, error);
    }
}
