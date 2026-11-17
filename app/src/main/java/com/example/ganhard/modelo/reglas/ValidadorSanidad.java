package com.example.ganhard.modelo.reglas;

import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.EventoSanitario;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reglas de la actividad sanitaria (RF11, RN05), en Java puro. Devuelve códigos, no textos. */
public final class ValidadorSanidad {
    public static final int DESCRIPCION_MAXIMO = 500, PRODUCTO_MAXIMO = 100, OBSERVACIONES_MAXIMO = 500;

    private ValidadorSanidad() {}

    public static Map<String, ErrorCampo> validar(EntradaSanidad entrada, Animal animal, LocalDate hoy) {
        Map<String, ErrorCampo> errores = new LinkedHashMap<>();
        if (entrada.tipo.isEmpty()) errores.put(Campos.TIPO, new ErrorCampo(CodigoError.OBLIGATORIO));
        else if (!EventoSanitario.TIPOS.contains(entrada.tipo)) errores.put(Campos.TIPO, new ErrorCampo(CodigoError.VALOR_INVALIDO));
        if (entrada.fecha.isEmpty()) {
            errores.put(Campos.FECHA, new ErrorCampo(CodigoError.OBLIGATORIO));
        } else {
            LocalDate fecha = Fechas.fecha(entrada.fecha);
            if (fecha == null) errores.put(Campos.FECHA, new ErrorCampo(CodigoError.FORMATO));
            // Un hecho realizado no puede ser futuro: eso se programa como tarea.
            else if (fecha.isAfter(hoy)) errores.put(Campos.FECHA, new ErrorCampo(CodigoError.FECHA_FUTURA));
            else {
                LocalDate nacimiento = Fechas.fecha(animal.fechaNacimiento);
                if (nacimiento != null && fecha.isBefore(nacimiento)) errores.put(Campos.FECHA, new ErrorCampo(CodigoError.FECHA_ANTERIOR));
            }
        }
        if (entrada.descripcion.isEmpty()) errores.put(Campos.DESCRIPCION, new ErrorCampo(CodigoError.OBLIGATORIO));
        else if (entrada.descripcion.length() > DESCRIPCION_MAXIMO) errores.put(Campos.DESCRIPCION, new ErrorCampo(CodigoError.LONGITUD_MAXIMA, DESCRIPCION_MAXIMO));
        if (entrada.producto.length() > PRODUCTO_MAXIMO) errores.put(Campos.PRODUCTO, new ErrorCampo(CodigoError.LONGITUD_MAXIMA, PRODUCTO_MAXIMO));
        if (entrada.observaciones.length() > OBSERVACIONES_MAXIMO) {
            errores.put(Campos.OBSERVACIONES, new ErrorCampo(CodigoError.LONGITUD_MAXIMA, OBSERVACIONES_MAXIMO));
        }
        return errores;
    }

    /** Un hecho anterior al ingreso a la finca es un antecedente. */
    public static boolean esAntecedente(String fecha, Animal animal) {
        LocalDate realizada = Fechas.fecha(fecha);
        LocalDate ingreso = Fechas.fecha(animal.fechaIngreso);
        return realizada != null && ingreso != null && realizada.isBefore(ingreso);
    }
}
