package com.example.ganhard.modelo.reglas;

/** Motivo de una corrección o anulación (RN06), común a todos los módulos: obligatorio y de hasta 500 caracteres. */
public final class ValidadorMotivo {
    public static final int MAXIMO = 500;

    private ValidadorMotivo() {}

    /** Null si el motivo es válido. */
    public static ErrorCampo validar(String motivo) {
        String texto = motivo == null ? "" : motivo.trim();
        if (texto.isEmpty()) return new ErrorCampo(CodigoError.OBLIGATORIO);
        if (texto.length() > MAXIMO) return new ErrorCampo(CodigoError.LONGITUD_MAXIMA, MAXIMO);
        return null;
    }
}
