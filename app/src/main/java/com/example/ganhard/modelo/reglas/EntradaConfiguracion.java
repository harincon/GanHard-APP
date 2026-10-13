package com.example.ganhard.modelo.reglas;

/** Datos del formulario. No es una tabla ni una cuenta autenticada. */
public final class EntradaConfiguracion {
    public final String nombre, correo, finca, ubicacion, area;
    public EntradaConfiguracion(String nombre, String correo, String finca, String ubicacion, String area) {
        this.nombre = limpiar(nombre);
        this.correo = limpiar(correo);
        this.finca = limpiar(finca);
        this.ubicacion = limpiar(ubicacion);
        this.area = limpiar(area);
    }
    private static String limpiar(String valor) { return valor == null ? "" : valor.trim(); }
}
