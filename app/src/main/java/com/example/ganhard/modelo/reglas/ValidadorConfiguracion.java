package com.example.ganhard.modelo.reglas;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reglas del Modelo, independientes de Android para poder probarlas. */
public final class ValidadorConfiguracion {
    private ValidadorConfiguracion() {}
    public static Map<String, String> validar(EntradaConfiguracion entrada) {
        Map<String, String> errores = new LinkedHashMap<>();
        obligatorio(errores, "nombre", entrada.nombre, 100, "Escribe el nombre del administrador.");
        obligatorio(errores, "correo", entrada.correo, 150, "Escribe un correo de contacto.");
        obligatorio(errores, "finca", entrada.finca, 100, "Escribe el nombre de la finca.");
        if (!entrada.correo.isEmpty() && !entrada.correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            errores.put("correo", "Revisa el formato del correo electrónico.");
        }
        if (entrada.ubicacion.length() > 200) errores.put("ubicacion", "Usa un máximo de 200 caracteres.");
        try { area(entrada.area); }
        catch (IllegalArgumentException ex) { errores.put("area", "Escribe un área positiva, con hasta 2 decimales y sin separadores de miles."); }
        return errores;
    }
    public static Double area(String valor) {
        if (valor.trim().isEmpty()) return null;
        if (!valor.matches("[0-9]{1,8}([.,][0-9]{1,2})?")) throw new IllegalArgumentException("Área inválida");
        BigDecimal area = new BigDecimal(valor.replace(',', '.'));
        if (area.signum() <= 0) throw new IllegalArgumentException("Área no positiva");
        return area.doubleValue();
    }
    private static void obligatorio(Map<String, String> errores, String campo, String valor, int maximo, String mensaje) {
        if (valor.isEmpty()) errores.put(campo, mensaje);
        else if (valor.length() > maximo) errores.put(campo, "Usa un máximo de " + maximo + " caracteres.");
    }
}
