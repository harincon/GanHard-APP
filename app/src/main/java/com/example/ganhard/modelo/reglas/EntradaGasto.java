package com.example.ganhard.modelo.reglas;

/**
 * Datos del formulario de gasto; todo es texto y lo ausente es la cadena vacía. El valor llega en pesos enteros,
 * solo con dígitos y sin puntos de miles. No es una tabla.
 */
public final class EntradaGasto {
    public final String fecha, categoria, concepto, valor;
    public EntradaGasto(String fecha, String categoria, String concepto, String valor) {
        this.fecha = limpiar(fecha);
        this.categoria = limpiar(categoria);
        this.concepto = limpiar(concepto);
        this.valor = limpiar(valor);
    }
    private static String limpiar(String valor) { return valor == null ? "" : valor.trim(); }
}
