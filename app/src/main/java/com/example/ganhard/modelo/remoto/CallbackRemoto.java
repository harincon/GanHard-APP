package com.example.ganhard.modelo.remoto;

/** Resultado de una operación remota, siempre entregado en el hilo principal. */
public interface CallbackRemoto<T> {
    void exito(T valor);
    void error(ErrorRemoto error);
}
