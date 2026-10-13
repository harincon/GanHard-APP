package com.example.ganhard;

import android.app.Application;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.room.Room;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.nucleo.EjecutorModelo;
import com.example.ganhard.modelo.repositorio.ConfiguracionRepository;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Una base, un ejecutor y los repositorios compartidos durante la vida del proceso. */
public class GanHardApplication extends Application {
    private EjecutorModelo ejecutor;
    private ConfiguracionRepository configuracion;
    @Override public void onCreate() {
        super.onCreate();
        int tema = getSharedPreferences("preferencias", MODE_PRIVATE).getInt("tema", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(tema);
        instalar(Room.databaseBuilder(this, GanHardDatabase.class, "ganhard.db").build(), Executors.newSingleThreadExecutor());
    }
    /** Arma los repositorios sobre una base y un hilo de trabajo; las pruebas lo usan con una base en memoria. */
    @VisibleForTesting public void instalar(GanHardDatabase database, Executor trabajo) {
        ejecutor = new EjecutorModelo(trabajo);
        configuracion = new ConfiguracionRepository(database.configuracionDao(), ejecutor);
    }
    public EjecutorModelo ejecutor() { return ejecutor; }
    public ConfiguracionRepository configuracion() { return configuracion; }
}
