package com.example.ganhard.modelo.reglas;

/** Datos del formulario de tarea; todo es texto y lo ausente es la cadena vacía. No es una tabla. */
public final class EntradaTarea {
    public final String tipo, idAnimal, titulo, descripcion, fechaProgramada;
    public EntradaTarea(String tipo, String idAnimal, String titulo, String descripcion, String fechaProgramada) {
        this.tipo = limpiar(tipo);
        this.idAnimal = limpiar(idAnimal);
        this.titulo = limpiar(titulo);
        this.descripcion = limpiar(descripcion);
        this.fechaProgramada = limpiar(fechaProgramada);
    }
    private static String limpiar(String valor) { return valor == null ? "" : valor.trim(); }
}
