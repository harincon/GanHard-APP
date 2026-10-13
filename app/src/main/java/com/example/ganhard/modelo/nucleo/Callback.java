package com.example.ganhard.modelo.nucleo;

/** Resultado de una operación del Modelo, siempre entregado en el hilo principal. */
public interface Callback<T> {
    void exito(T valor);
    void error(ErrorModelo error);
}
