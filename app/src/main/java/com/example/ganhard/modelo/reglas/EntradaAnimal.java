package com.example.ganhard.modelo.reglas;

/** Datos del formulario de animal; todo es texto y lo ausente es la cadena vacía. No es una tabla. */
public final class EntradaAnimal {
    public final String codigo, nombre, sexo, idRaza, fechaNacimiento, fechaIngreso, observaciones;
    public EntradaAnimal(String codigo, String nombre, String sexo, String idRaza,
                         String fechaNacimiento, String fechaIngreso, String observaciones) {
        this.codigo = limpiar(codigo);
        this.nombre = limpiar(nombre);
        this.sexo = limpiar(sexo);
        this.idRaza = limpiar(idRaza);
        this.fechaNacimiento = limpiar(fechaNacimiento);
        this.fechaIngreso = limpiar(fechaIngreso);
        this.observaciones = limpiar(observaciones);
    }
    private static String limpiar(String valor) { return valor == null ? "" : valor.trim(); }
}
