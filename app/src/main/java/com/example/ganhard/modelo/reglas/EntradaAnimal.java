package com.example.ganhard.modelo.reglas;

import java.util.List;

/** Datos del formulario de animal; todo es texto y lo ausente es la cadena vacía. No es una tabla. */
public final class EntradaAnimal {
    public final String codigo, nombre, sexo, idRaza, fechaNacimiento, fechaIngreso, observaciones;
    /**
     * Raza tal como la escribió la persona. Nulo significa que no se usa y vale {@link #idRaza}. Si viene, el Modelo
     * busca una raza del catálogo con ese nombre (sin distinguir mayúsculas ni tildes) o agrega una nueva; vacío es
     * "sin raza".
     */
    public final String razaEscrita;
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
        this(codigo, nombre, sexo, idRaza, null, fechaNacimiento, fechaIngreso, observaciones, identificadores);
    }

    private EntradaAnimal(String codigo, String nombre, String sexo, String idRaza, String razaEscrita, String fechaNacimiento,
                          String fechaIngreso, String observaciones, List<EntradaIdentificador> identificadores) {
        this.codigo = limpiar(codigo);
        this.nombre = limpiar(nombre);
        this.sexo = limpiar(sexo);
        this.idRaza = limpiar(idRaza);
        this.razaEscrita = razaEscrita == null ? null : limpiar(razaEscrita);
        this.fechaNacimiento = limpiar(fechaNacimiento);
        this.fechaIngreso = limpiar(fechaIngreso);
        this.observaciones = limpiar(observaciones);
        this.identificadores = identificadores;
    }

    /** La misma entrada con la raza escrita a mano en lugar de una elegida por id. */
    public EntradaAnimal conRazaEscrita(String raza) {
        return new EntradaAnimal(codigo, nombre, sexo, "", raza == null ? "" : raza, fechaNacimiento, fechaIngreso, observaciones, identificadores);
    }

    private static String limpiar(String valor) { return valor == null ? "" : valor.trim(); }
}
