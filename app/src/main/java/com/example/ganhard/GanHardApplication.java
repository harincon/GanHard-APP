package com.example.ganhard;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.room.Room;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.repositorio.ConfiguracionRepository;

/** Una base y un repositorio compartidos durante la vida del proceso. */
public class GanHardApplication extends Application {
    private ConfiguracionRepository configuracion;
    @Override public void onCreate() {
        super.onCreate();
        int tema = getSharedPreferences("preferencias", MODE_PRIVATE).getInt("tema", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(tema);
        GanHardDatabase database = Room.databaseBuilder(this, GanHardDatabase.class, "ganhard.db").build();
        configuracion = new ConfiguracionRepository(database.configuracionDao());
    }
    public ConfiguracionRepository configuracion() { return configuracion; }
}
