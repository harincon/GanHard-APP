package com.example.ganhard.modelo.nucleo;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executor;

/**
 * Único punto por donde el Modelo trabaja fuera del hilo de pantalla. Todas las operaciones comparten
 * un hilo, así que se ejecutan en el orden en que se solicitan; el resultado vuelve al hilo principal.
 */
public final class EjecutorModelo {
    public interface Tarea<T> {
        T ejecutar() throws Exception;
    }

    private final Executor trabajo;
    private final Handler principal = new Handler(Looper.getMainLooper());

    public EjecutorModelo(Executor trabajo) { this.trabajo = trabajo; }

    public <T> void ejecutar(Tarea<T> tarea, Callback<T> callback) {
        trabajo.execute(() -> {
            try {
                T valor = tarea.ejecutar();
                principal.post(() -> callback.exito(valor));
            } catch (ErrorModelo ex) {
                principal.post(() -> callback.error(ex));
            } catch (Exception ex) {
                ErrorModelo error = ErrorModelo.baseDeDatos(ex);
                principal.post(() -> callback.error(error));
            }
        });
    }
}
