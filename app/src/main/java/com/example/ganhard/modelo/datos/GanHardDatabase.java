package com.example.ganhard.modelo.datos;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.Usuario;

/** Versión 1: configuración. Los módulos siguientes requerirán migraciones. */
@Database(entities = {Usuario.class, Finca.class}, version = 1, exportSchema = true)
public abstract class GanHardDatabase extends RoomDatabase {
    public abstract ConfiguracionDao configuracionDao();
}
