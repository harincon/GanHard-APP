package com.example.ganhard.modelo.datos;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.Pesaje;
import com.example.ganhard.modelo.entidad.Raza;
import com.example.ganhard.modelo.entidad.Usuario;

/** Versión 2: configuración, catálogo de razas, animales y pesajes. */
@Database(entities = {Usuario.class, Finca.class, Raza.class, Animal.class, Pesaje.class}, version = 2, exportSchema = true)
public abstract class GanHardDatabase extends RoomDatabase {
    public abstract ConfiguracionDao configuracionDao();
    public abstract RazaDao razaDao();
    public abstract AnimalDao animalDao();

    /** Constructor con las migraciones y la siembra de razas; con archivo nulo la base es en memoria. */
    public static Builder<GanHardDatabase> constructor(Context contexto, String archivo) {
        Builder<GanHardDatabase> constructor = archivo == null
                ? Room.inMemoryDatabaseBuilder(contexto, GanHardDatabase.class)
                : Room.databaseBuilder(contexto, GanHardDatabase.class, archivo);
        return constructor.addMigrations(Migraciones.V1_V2).addCallback(new Callback() {
            @Override public void onCreate(@NonNull SupportSQLiteDatabase db) { CatalogoRazas.insertar(db); }
        });
    }
}
