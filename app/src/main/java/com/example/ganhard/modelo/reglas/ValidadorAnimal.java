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

    public static Map<String, ErrorCampo> validar(EntradaAnimal entrada, LocalDate hoy) { return validar(entrada, hoy, true); }

    /** Con {@code codigoObligatorio} falso (al registrar) el código puede ir vacío: el Modelo asigna el siguiente. */
    public static Map<String, ErrorCampo> validar(EntradaAnimal entrada, LocalDate hoy, boolean codigoObligatorio) {
        Map<String, ErrorCampo> errores = new LinkedHashMap<>();
        if (entrada.codigo.isEmpty()) { if (codigoObligatorio) errores.put(Campos.CODIGO, new ErrorCampo(CodigoError.OBLIGATORIO)); }
        else if (entrada.codigo.length() > CODIGO_MAXIMO) errores.put(Campos.CODIGO, new ErrorCampo(CodigoError.LONGITUD_MAXIMA, CODIGO_MAXIMO));
        if (entrada.nombre.length() > NOMBRE_MAXIMO) errores.put(Campos.NOMBRE, new ErrorCampo(CodigoError.LONGITUD_MAXIMA, NOMBRE_MAXIMO));
        if (entrada.sexo.isEmpty()) errores.put(Campos.SEXO, new ErrorCampo(CodigoError.OBLIGATORIO));
        else if (!Animal.HEMBRA.equals(entrada.sexo) && !Animal.MACHO.equals(entrada.sexo)) errores.put(Campos.SEXO, new ErrorCampo(CodigoError.VALOR_INVALIDO));

        LocalDate nacimiento = null;
        if (!entrada.fechaNacimiento.isEmpty()) {
            nacimiento = Fechas.fecha(entrada.fechaNacimiento);
            if (nacimiento == null) errores.put(Campos.FECHA_NACIMIENTO, new ErrorCampo(CodigoError.FORMATO));
            else if (nacimiento.isAfter(hoy)) errores.put(Campos.FECHA_NACIMIENTO, new ErrorCampo(CodigoError.FECHA_FUTURA));
        }
        if (entrada.fechaIngreso.isEmpty()) {
            errores.put(Campos.FECHA_INGRESO, new ErrorCampo(CodigoError.OBLIGATORIO));
        } else {
            LocalDate ingreso = Fechas.fecha(entrada.fechaIngreso);
            if (ingreso == null) errores.put(Campos.FECHA_INGRESO, new ErrorCampo(CodigoError.FORMATO));
            else if (ingreso.isAfter(hoy)) errores.put(Campos.FECHA_INGRESO, new ErrorCampo(CodigoError.FECHA_FUTURA));
            else if (nacimiento != null && !errores.containsKey(Campos.FECHA_NACIMIENTO) && ingreso.isBefore(nacimiento)) {
                errores.put(Campos.FECHA_INGRESO, new ErrorCampo(CodigoError.FECHA_ANTERIOR));
            }
        }
        if (entrada.observaciones.length() > OBSERVACIONES_MAXIMO) {
            errores.put(Campos.OBSERVACIONES, new ErrorCampo(CodigoError.LONGITUD_MAXIMA, OBSERVACIONES_MAXIMO));
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
