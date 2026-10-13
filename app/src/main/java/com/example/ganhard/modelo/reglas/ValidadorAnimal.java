package com.example.ganhard.modelo.reglas;

import com.example.ganhard.modelo.entidad.Animal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reglas del animal (RF03, RN03, RN05, RNF06), en Java puro. Devuelve códigos, no textos. */
public final class ValidadorAnimal {
    public static final int CODIGO_MAXIMO = 30, NOMBRE_MAXIMO = 100, OBSERVACIONES_MAXIMO = 500;

    private ValidadorAnimal() {}

    /** El código se guarda sin espacios en los extremos; la unicidad por finca ignora mayúsculas. */
    public static String normalizarCodigo(String codigo) { return codigo == null ? "" : codigo.trim(); }

    public static Map<String, ErrorCampo> validar(EntradaAnimal entrada, LocalDate hoy) {
        Map<String, ErrorCampo> errores = new LinkedHashMap<>();
        if (entrada.codigo.isEmpty()) errores.put("codigo", new ErrorCampo(CodigoError.OBLIGATORIO));
        else if (entrada.codigo.length() > CODIGO_MAXIMO) errores.put("codigo", new ErrorCampo(CodigoError.LONGITUD_MAXIMA, CODIGO_MAXIMO));
        if (entrada.nombre.length() > NOMBRE_MAXIMO) errores.put("nombre", new ErrorCampo(CodigoError.LONGITUD_MAXIMA, NOMBRE_MAXIMO));
        if (entrada.sexo.isEmpty()) errores.put("sexo", new ErrorCampo(CodigoError.OBLIGATORIO));
        else if (!Animal.HEMBRA.equals(entrada.sexo) && !Animal.MACHO.equals(entrada.sexo)) errores.put("sexo", new ErrorCampo(CodigoError.VALOR_INVALIDO));

        LocalDate nacimiento = null;
        if (!entrada.fechaNacimiento.isEmpty()) {
            nacimiento = Fechas.fecha(entrada.fechaNacimiento);
            if (nacimiento == null) errores.put("fechaNacimiento", new ErrorCampo(CodigoError.FORMATO));
            else if (nacimiento.isAfter(hoy)) errores.put("fechaNacimiento", new ErrorCampo(CodigoError.FECHA_FUTURA));
        }
        if (entrada.fechaIngreso.isEmpty()) {
            errores.put("fechaIngreso", new ErrorCampo(CodigoError.OBLIGATORIO));
        } else {
            LocalDate ingreso = Fechas.fecha(entrada.fechaIngreso);
            if (ingreso == null) errores.put("fechaIngreso", new ErrorCampo(CodigoError.FORMATO));
            else if (ingreso.isAfter(hoy)) errores.put("fechaIngreso", new ErrorCampo(CodigoError.FECHA_FUTURA));
            else if (nacimiento != null && !errores.containsKey("fechaNacimiento") && ingreso.isBefore(nacimiento)) {
                errores.put("fechaIngreso", new ErrorCampo(CodigoError.FECHA_ANTERIOR));
            }
        }
        if (entrada.observaciones.length() > OBSERVACIONES_MAXIMO) {
            errores.put("observaciones", new ErrorCampo(CodigoError.LONGITUD_MAXIMA, OBSERVACIONES_MAXIMO));
        }
        return errores;
    }

    /** Fecha efectiva de un cambio de estado: obligatoria, no futura y no anterior al ingreso. Null si es válida. */
    public static ErrorCampo fechaEstado(String fecha, Animal animal, LocalDate hoy) {
        String texto = fecha == null ? "" : fecha.trim();
        if (texto.isEmpty()) return new ErrorCampo(CodigoError.OBLIGATORIO);
        LocalDate valor = Fechas.fecha(texto);
        if (valor == null) return new ErrorCampo(CodigoError.FORMATO);
        if (valor.isAfter(hoy)) return new ErrorCampo(CodigoError.FECHA_FUTURA);
        LocalDate ingreso = Fechas.fecha(animal.fechaIngreso);
        if (ingreso != null && valor.isBefore(ingreso)) return new ErrorCampo(CodigoError.FECHA_ANTERIOR);
        return null;
    }
}
