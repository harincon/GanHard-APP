package com.example.ganhard.pruebas;

import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;

/** Recoge el resultado de un Callback del Modelo en pruebas con ejecutor directo y hilo principal pausado. */
public final class Captura<T> implements Callback<T> {
    public T valor;
    public ErrorModelo error;
    public boolean exito, terminado;

    @Override public void exito(T valor) { this.valor = valor; this.exito = true; this.terminado = true; }
    @Override public void error(ErrorModelo error) { this.error = error; this.terminado = true; }

    /** Entrega los mensajes pendientes al hilo principal y devuelve this para encadenar. */
    public Captura<T> esperar() {
        shadowOf(Looper.getMainLooper()).idle();
        if (!terminado) throw new AssertionError("El Modelo no entregó ningún resultado");
        return this;
    }
}
