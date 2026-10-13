package com.example.ganhard.modelo.reglas;

/** Motivo de un error de validación. La Vista lo traduce a un texto de recursos. */
public enum CodigoError {
    OBLIGATORIO, LONGITUD_MAXIMA, FORMATO, VALOR_INVALIDO, FUERA_DE_RANGO, FECHA_FUTURA, FECHA_ANTERIOR, DUPLICADO
}
