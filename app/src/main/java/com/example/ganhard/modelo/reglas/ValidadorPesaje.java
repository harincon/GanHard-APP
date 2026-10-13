package com.example.ganhard.modelo.reglas;

import com.example.ganhard.modelo.entidad.Animal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reglas del pesaje (RF08, RN05, RNF06), en Java puro. Devuelve códigos, no textos. */
public final class ValidadorPesaje {
    /** Tope práctico para detectar errores de digitación; el diccionario solo limita a DECIMAL(8,2). */
    public static final int PESO_MAXIMO_KG = 2000;
    public static final int OBSERVACIONES_MAXIMO = 500;
    private static final BigDecimal MAXIMO = new BigDecimal(PESO_MAXIMO_KG);

    private ValidadorPesaje() {}

    public static Map<String, ErrorCampo> validar(EntradaPesaje entrada, Animal animal, LocalDateTime ahora) {
        Map<String, ErrorCampo> errores = new LinkedHashMap<>();
        LocalDate hoy = ahora.toLocalDate();

        LocalDate fecha = null;
        if (entrada.fecha.isEmpty()) {
            errores.put("fecha", new ErrorCampo(CodigoError.OBLIGATORIO));
        } else {
            fecha = Fechas.fecha(entrada.fecha);
            if (fecha == null) errores.put("fecha", new ErrorCampo(CodigoError.FORMATO));
            else if (fecha.isAfter(hoy)) errores.put("fecha", new ErrorCampo(CodigoError.FECHA_FUTURA));
            else {
                LocalDate nacimiento = Fechas.fecha(animal.fechaNacimiento);
                if (nacimiento != null && fecha.isBefore(nacimiento)) errores.put("fecha", new ErrorCampo(CodigoError.FECHA_ANTERIOR));
            }
        }

        if (entrada.hora.isEmpty()) {
            // Solo un antecedente anterior al ingreso puede omitir la hora.
            if (!esAntecedente(entrada.fecha, animal)) errores.put("hora", new ErrorCampo(CodigoError.OBLIGATORIO));
        } else {
            LocalTime hora = Fechas.hora(entrada.hora);
            if (hora == null) errores.put("hora", new ErrorCampo(CodigoError.FORMATO));
            else if (fecha != null && fecha.equals(hoy) && hora.isAfter(ahora.toLocalTime())) {
                errores.put("hora", new ErrorCampo(CodigoError.FECHA_FUTURA));
            }
        }

        ErrorCampo peso = errorPeso(entrada.pesoKg);
        if (peso != null) errores.put("peso", peso);
        if (entrada.observaciones.length() > OBSERVACIONES_MAXIMO) {
            errores.put("observaciones", new ErrorCampo(CodigoError.LONGITUD_MAXIMA, OBSERVACIONES_MAXIMO));
        }
        return errores;
    }

    /** Peso en kg; acepta coma o punto decimal. Lanza IllegalArgumentException si no es válido. */
    public static Double peso(String valor) {
        String texto = valor == null ? "" : valor.trim();
        ErrorCampo error = errorPeso(texto);
        if (error != null) throw new IllegalArgumentException("Peso inválido: " + error.codigo);
        return new BigDecimal(texto.replace(',', '.')).doubleValue();
    }

    /** Un hecho anterior al ingreso a la finca es un antecedente. */
    public static boolean esAntecedente(String fecha, Animal animal) {
        LocalDate medida = Fechas.fecha(fecha);
        LocalDate ingreso = Fechas.fecha(animal.fechaIngreso);
        return medida != null && ingreso != null && medida.isBefore(ingreso);
    }

    private static ErrorCampo errorPeso(String valor) {
        if (valor.isEmpty()) return new ErrorCampo(CodigoError.OBLIGATORIO);
        if (!valor.matches("[0-9]+([.,][0-9]+)?")) return new ErrorCampo(CodigoError.VALOR_INVALIDO);
        // Más de 2 decimales (o un punto de miles como "1.050") no se interpreta: se pide corregir.
        if (!valor.matches("[0-9]+([.,][0-9]{1,2})?")) return new ErrorCampo(CodigoError.FORMATO);
        BigDecimal peso = new BigDecimal(valor.replace(',', '.'));
        if (peso.signum() <= 0) return new ErrorCampo(CodigoError.VALOR_INVALIDO);
        if (peso.compareTo(MAXIMO) > 0) return new ErrorCampo(CodigoError.FUERA_DE_RANGO);
        return null;
    }
}
