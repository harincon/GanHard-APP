package com.example.ganhard;

import android.app.Application;
import androidx.annotation.VisibleForTesting;
import com.example.ganhard.modelo.PreferenciaTema;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.nucleo.EjecutorModelo;
import com.example.ganhard.modelo.repositorio.AnimalRepository;
import com.example.ganhard.modelo.repositorio.ConfiguracionRepository;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Una base, un ejecutor y los repositorios compartidos durante la vida del proceso. */
public class GanHardApplication extends Application {
    private EjecutorModelo ejecutor;
    private ConfiguracionRepository configuracion;
    private AnimalRepository animales;
    @Override public void onCreate() {
        super.onCreate();
        PreferenciaTema.aplicar(this);
        instalar(GanHardDatabase.constructor(this, "ganhard.db").build(), Executors.newSingleThreadExecutor());
    }
    /** Arma los repositorios sobre una base y un hilo de trabajo; las pruebas lo usan con una base en memoria. */
    @VisibleForTesting public void instalar(GanHardDatabase database, Executor trabajo) {
        ejecutor = new EjecutorModelo(trabajo);
        configuracion = new ConfiguracionRepository(database.configuracionDao(), ejecutor);
        animales = new AnimalRepository(database, ejecutor);
    }
    public EjecutorModelo ejecutor() { return ejecutor; }
    public ConfiguracionRepository configuracion() { return configuracion; }
    public AnimalRepository animales() { return animales; }
}
