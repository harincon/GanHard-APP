package com.example.ganhard.modelo.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.ganhard.modelo.entidad.MarcaSincronizacion;
import java.util.List;

/** Las marcas del respaldo: una por tabla subida, más el resumen de la última sincronización. */
@Dao
public abstract class MarcaSincronizacionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void guardar(MarcaSincronizacion marca);

    @Query("SELECT * FROM marca_sincronizacion WHERE tabla = :tabla")
    public abstract MarcaSincronizacion de(String tabla);

    @Query("SELECT * FROM marca_sincronizacion")
    public abstract List<MarcaSincronizacion> todas();

    /** Olvida lo subido de las tablas de datos (no el resumen ni lo bajado): la próxima vez se sube todo otra vez. */
    @Query("DELETE FROM marca_sincronizacion WHERE tabla NOT LIKE 'bajada:%' AND tabla <> '_resumen'")
    public abstract void borrarMarcasDeSubida();
}
