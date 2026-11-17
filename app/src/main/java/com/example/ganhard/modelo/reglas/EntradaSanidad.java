package com.example.ganhard.modelo.reglas;

/** Datos del formulario de actividad sanitaria; todo es texto y lo ausente es la cadena vacía. No es una tabla. */
public final class EntradaSanidad {
    public final String tipo, fecha, descripcion, producto, observaciones;
    public EntradaSanidad(String tipo, String fecha, String descripcion, String producto, String observaciones) {
        this.tipo = limpiar(tipo);
        this.fecha = limpiar(fecha);
        this.descripcion = limpiar(descripcion);
        this.producto = limpiar(producto);
        this.observaciones = limpiar(observaciones);
    }
    private static String limpiar(String valor) { return valor == null ? "" : valor.trim(); }
}
