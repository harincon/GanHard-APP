package com.example.ganhard.modelo.datos;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.EventoSanitario;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.Gasto;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import com.example.ganhard.modelo.entidad.Pesaje;
import com.example.ganhard.modelo.entidad.Tarea;
import com.example.ganhard.modelo.entidad.Raza;
import com.example.ganhard.modelo.entidad.Usuario;

/** Versión 6: configuración, catálogo de razas, animales, pesajes, historial de cambios, sanidad, gastos y tareas. */
@Database(entities = {Usuario.class, Finca.class, Raza.class, Animal.class, Pesaje.class, HistorialCambio.class, EventoSanitario.class, Gasto.class, Tarea.class}, version = 6, exportSchema = true)
public abstract class GanHardDatabase extends RoomDatabase {
    public abstract ConfiguracionDao configuracionDao();
    public abstract RazaDao razaDao();
    public abstract AnimalDao animalDao();
    public abstract PesajeDao pesajeDao();
    public abstract HistorialDao historialDao();
    public abstract EventoSanitarioDao eventoSanitarioDao();
    public abstract GastoDao gastoDao();
    public abstract TareaDao tareaDao();

    /** Constructor con las migraciones y la siembra de razas; con archivo nulo la base es en memoria. */
    public static Builder<GanHardDatabase> constructor(Context contexto, String archivo) {
        Builder<GanHardDatabase> constructor = archivo == null
                ? Room.inMemoryDatabaseBuilder(contexto, GanHardDatabase.class)
                : Room.databaseBuilder(contexto, GanHardDatabase.class, archivo);
        return constructor.addMigrations(Migraciones.V1_V2, Migraciones.V2_V3, Migraciones.V3_V4, Migraciones.V4_V5, Migraciones.V5_V6).addCallback(new Callback() {
            @Override public void onCreate(@NonNull SupportSQLiteDatabase db) { CatalogoRazas.insertar(db); }
        });
    }
}
