package com.example.ganhard.modelo.reglas;

import com.example.ganhard.modelo.entidad.AnimalIdentificador;

/**
 * Un identificador tal como llega del formulario: un HIERRO lleva el id del hierro del catálogo; un ARETE o un
 * NUMERO lleva su valor escrito. Lo que no corresponde al tipo queda vacío.
 */
public final class EntradaIdentificador {
    public final String tipo, idHierro, valor;

    public EntradaIdentificador(String tipo, String idHierro, String valor) {
        this.tipo = limpiar(tipo);
        boolean esHierro = AnimalIdentificador.HIERRO.equals(this.tipo);
        this.idHierro = esHierro ? limpiar(idHierro) : "";
        this.valor = esHierro ? "" : limpiar(valor);
    }

    public boolean esHierro() { return AnimalIdentificador.HIERRO.equals(tipo); }

    private static String limpiar(String texto) { return texto == null ? "" : texto.trim(); }
}
