package com.example.ganhard.modelo.reglas;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reglas del Modelo, independientes de Android para poder probarlas. Devuelve códigos, no textos. */
public final class ValidadorConfiguracion {
    private ValidadorConfiguracion() {}
    public static Map<String, ErrorCampo> validar(EntradaConfiguracion entrada) {
        Map<String, ErrorCampo> errores = new LinkedHashMap<>();
        obligatorio(errores, "nombre", entrada.nombre, 100);
        obligatorio(errores, "correo", entrada.correo, 150);
        obligatorio(errores, "finca", entrada.finca, 100);
        if (!entrada.correo.isEmpty() && !entrada.correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            errores.put("correo", new ErrorCampo(CodigoError.FORMATO));
        }
        if (entrada.ubicacion.length() > 200) errores.put("ubicacion", new ErrorCampo(CodigoError.LONGITUD_MAXIMA, 200));
        try { area(entrada.area); }
        catch (IllegalArgumentException ex) { errores.put("area", new ErrorCampo(CodigoError.VALOR_INVALIDO)); }
        return errores;
    }
    public static Double area(String valor) {
        if (valor.trim().isEmpty()) return null;
        if (!valor.matches("[0-9]{1,8}([.,][0-9]{1,2})?")) throw new IllegalArgumentException("Área inválida");
        BigDecimal area = new BigDecimal(valor.replace(',', '.'));
        if (area.signum() <= 0) throw new IllegalArgumentException("Área no positiva");
        return area.doubleValue();
    }
    private static void obligatorio(Map<String, ErrorCampo> errores, String campo, String valor, int maximo) {
        if (valor.isEmpty()) errores.put(campo, new ErrorCampo(CodigoError.OBLIGATORIO));
        else if (valor.length() > maximo) errores.put(campo, new ErrorCampo(CodigoError.LONGITUD_MAXIMA, maximo));
    }
}
