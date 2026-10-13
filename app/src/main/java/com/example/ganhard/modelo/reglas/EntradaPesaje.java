package com.example.ganhard.modelo.reglas;

/** Datos del formulario de pesaje; todo es texto y lo ausente es la cadena vacía. No es una tabla. */
public final class EntradaPesaje {
    public final String fecha, hora, pesoKg, observaciones;
    public EntradaPesaje(String fecha, String hora, String pesoKg, String observaciones) {
        this.fecha = limpiar(fecha);
        this.hora = limpiar(hora);
        this.pesoKg = limpiar(pesoKg);
        this.observaciones = limpiar(observaciones);
    }
    private static String limpiar(String valor) { return valor == null ? "" : valor.trim(); }
}
