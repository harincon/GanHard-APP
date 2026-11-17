package com.example.ganhard.modelo.nucleo;

import com.example.ganhard.modelo.reglas.CodigoError;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import java.util.Collections;
import java.util.Map;

/** Fallo de una operación del Modelo, con un tipo que la Vista puede distinguir sin leer mensajes. */
public final class ErrorModelo extends Exception {
    public enum Tipo { VALIDACION, DUPLICADO, NO_ENCONTRADO, BASE_DE_DATOS }

    /** Lo que puede faltar en un error NO_ENCONTRADO. */
    public static final String FINCA = "finca", ANIMAL = "animal", PESAJE = "pesaje", EVENTO = "evento_sanitario", GASTO = "gasto", TAREA = "tarea", HIERRO = "hierro";

    public final Tipo tipo;
    /** Errores por campo, con las mismas claves que usa la Vista; vacío si el error no es de un campo. */
    public final Map<String, ErrorCampo> campos;
    /** En NO_ENCONTRADO, qué faltó (FINCA, ANIMAL, PESAJE, EVENTO, GASTO o TAREA); nulo en los demás tipos. */
    public final String faltante;

    private ErrorModelo(Tipo tipo, String mensaje, Throwable causa, Map<String, ErrorCampo> campos, String faltante) {
        super(mensaje, causa);
        this.tipo = tipo;
        this.campos = Collections.unmodifiableMap(campos);
        this.faltante = faltante;
    }

    public static ErrorModelo validacion(Map<String, ErrorCampo> campos) {
        return new ErrorModelo(Tipo.VALIDACION, "Datos inválidos", null, campos, null);
    }
    public static ErrorModelo duplicado(String campo) {
        return new ErrorModelo(Tipo.DUPLICADO, "Valor duplicado en " + campo, null,
                Collections.singletonMap(campo, new ErrorCampo(CodigoError.DUPLICADO)), null);
    }
    public static ErrorModelo noEncontrado(String faltante) {
        return new ErrorModelo(Tipo.NO_ENCONTRADO, "No se encontró " + faltante, null, Collections.emptyMap(), faltante);
    }
    public static ErrorModelo baseDeDatos(Throwable causa) {
        return new ErrorModelo(Tipo.BASE_DE_DATOS, "Falló la base de datos", causa, Collections.emptyMap(), null);
    }
}
