package com.example.ganhard.modelo.remoto;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executor;

/**
 * Hilo de trabajo de las operaciones con el servidor. Es distinto del de la base local para que una red lenta no
 * retrase las pantallas de trabajo. El resultado vuelve al hilo principal.
 */
public final class EjecutorRemoto {
    public interface Tarea<T> {
        T ejecutar() throws Exception;
    }

    private final Executor trabajo;
    private final Handler principal = new Handler(Looper.getMainLooper());

    public EjecutorRemoto(Executor trabajo) { this.trabajo = trabajo; }

    public <T> void ejecutar(Tarea<T> tarea, CallbackRemoto<T> callback) {
        trabajo.execute(() -> {
            try {
                T valor = tarea.ejecutar();
                principal.post(() -> callback.exito(valor));
            } catch (ErrorRemoto ex) {
                principal.post(() -> callback.error(ex));
            } catch (Exception ex) {
                ErrorRemoto error = ErrorRemoto.de(ErrorRemoto.Tipo.LOCAL, null, "Falló la base de datos", ex);
                principal.post(() -> callback.error(error));
            }
        });
    }
}
