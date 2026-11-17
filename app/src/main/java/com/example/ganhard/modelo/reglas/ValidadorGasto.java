package com.example.ganhard.modelo.reglas;

import com.example.ganhard.modelo.entidad.Gasto;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reglas del gasto (RF13, RN05), en Java puro. Devuelve códigos, no textos. */
public final class ValidadorGasto {
    public static final int CONCEPTO_MAXIMO = 200;
    /** Dígitos máximos del valor en pesos: 999.999.999.999, para que ninguna suma desborde los 64 bits. */
    public static final int VALOR_MAXIMO_DIGITOS = 12;

    private ValidadorGasto() {}

    public static Map<String, ErrorCampo> validar(EntradaGasto entrada, LocalDate hoy) {
        Map<String, ErrorCampo> errores = new LinkedHashMap<>();
        if (entrada.fecha.isEmpty()) {
            errores.put(Campos.FECHA, new ErrorCampo(CodigoError.OBLIGATORIO));
        } else {
            LocalDate fecha = Fechas.fecha(entrada.fecha);
            if (fecha == null) errores.put(Campos.FECHA, new ErrorCampo(CodigoError.FORMATO));
            else if (fecha.isAfter(hoy)) errores.put(Campos.FECHA, new ErrorCampo(CodigoError.FECHA_FUTURA));
        }
        if (entrada.categoria.isEmpty()) errores.put(Campos.CATEGORIA, new ErrorCampo(CodigoError.OBLIGATORIO));
        else if (!Gasto.CATEGORIAS.contains(entrada.categoria)) errores.put(Campos.CATEGORIA, new ErrorCampo(CodigoError.VALOR_INVALIDO));
        if (entrada.concepto.isEmpty()) errores.put(Campos.CONCEPTO, new ErrorCampo(CodigoError.OBLIGATORIO));
        else if (entrada.concepto.length() > CONCEPTO_MAXIMO) errores.put(Campos.CONCEPTO, new ErrorCampo(CodigoError.LONGITUD_MAXIMA, CONCEPTO_MAXIMO));
        ErrorCampo valor = errorValor(entrada.valor);
        if (valor != null) errores.put(Campos.VALOR, valor);
        return errores;
    }

    /** Valor en centavos a partir de pesos enteros ya validados. Lanza IllegalArgumentException si no es válido. */
    public static long centavos(String valor) {
        String texto = valor == null ? "" : valor.trim();
        ErrorCampo error = errorValor(texto);
        if (error != null) throw new IllegalArgumentException("Valor inválido: " + error.codigo);
        return Long.parseLong(sinCeros(texto)) * 100L;
    }

    private static ErrorCampo errorValor(String valor) {
        if (valor.isEmpty()) return new ErrorCampo(CodigoError.OBLIGATORIO);
        if (!valor.matches("[0-9]+")) return new ErrorCampo(CodigoError.VALOR_INVALIDO);
        String pesos = sinCeros(valor);
        if (pesos.isEmpty()) return new ErrorCampo(CodigoError.VALOR_INVALIDO);
        if (pesos.length() > VALOR_MAXIMO_DIGITOS) return new ErrorCampo(CodigoError.FUERA_DE_RANGO);
        return null;
    }

    private static String sinCeros(String digitos) { return digitos.replaceFirst("^0+", ""); }
}
