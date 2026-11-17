package com.example.ganhard.modelo.reglas;

/** Reglas del hierro del catálogo: nombre obligatorio de hasta 60 caracteres y descripción opcional de hasta 200. */
public final class ValidadorHierro {
    public static final int NOMBRE_MAXIMO = 60, DESCRIPCION_MAXIMO = 200;

    private ValidadorHierro() {}

    /** Null si el nombre es válido. La unicidad por finca la comprueba el repositorio. */
    public static ErrorCampo nombre(String nombre) {
        String texto = nombre == null ? "" : nombre.trim();
        if (texto.isEmpty()) return new ErrorCampo(CodigoError.OBLIGATORIO);
        if (texto.length() > NOMBRE_MAXIMO) return new ErrorCampo(CodigoError.LONGITUD_MAXIMA, NOMBRE_MAXIMO);
        return null;
    }

    /** Null si la descripción es válida (vacía también lo es). */
    public static ErrorCampo descripcion(String descripcion) {
        String texto = descripcion == null ? "" : descripcion.trim();
        return texto.length() > DESCRIPCION_MAXIMO ? new ErrorCampo(CodigoError.LONGITUD_MAXIMA, DESCRIPCION_MAXIMO) : null;
    }
}
