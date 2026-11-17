package com.example.ganhard.modelo.reglas;

import java.util.List;

/** Datos del formulario de animal; todo es texto y lo ausente es la cadena vacía. No es una tabla. */
public final class EntradaAnimal {
    public final String codigo, nombre, sexo, idRaza, fechaNacimiento, fechaIngreso, observaciones;
    /**
     * Identificadores (hierros, aretes, números) del animal en el orden de la pantalla. Nulo significa que quien
     * llama no los gestiona: no se exige hierro y, al editar, los que el animal ya tiene no se tocan.
     */
    public final List<EntradaIdentificador> identificadores;

    public EntradaAnimal(String codigo, String nombre, String sexo, String idRaza,
                         String fechaNacimiento, String fechaIngreso, String observaciones) {
        this(codigo, nombre, sexo, idRaza, fechaNacimiento, fechaIngreso, observaciones, null);
    }

    public EntradaAnimal(String codigo, String nombre, String sexo, String idRaza, String fechaNacimiento,
                         String fechaIngreso, String observaciones, List<EntradaIdentificador> identificadores) {
        this.codigo = limpiar(codigo);
        this.nombre = limpiar(nombre);
        this.sexo = limpiar(sexo);
        this.idRaza = limpiar(idRaza);
        this.fechaNacimiento = limpiar(fechaNacimiento);
        this.fechaIngreso = limpiar(fechaIngreso);
        this.observaciones = limpiar(observaciones);
        this.identificadores = identificadores;
    }
    private static String limpiar(String valor) { return valor == null ? "" : valor.trim(); }
}
