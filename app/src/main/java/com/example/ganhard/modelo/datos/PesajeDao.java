package com.example.ganhard.modelo.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.ganhard.modelo.entidad.Pesaje;
import java.util.List;

/** Pesajes vigentes de un animal: los anulados se conservan en la tabla pero no aparecen aquí (RN07). */
@Dao
public abstract class PesajeDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    public abstract void insertar(Pesaje pesaje);

    /** Del más reciente al más antiguo por fecha y hora; sin hora cuenta como la más temprana del día. */
    @Query("SELECT * FROM pesaje WHERE id_animal = :idAnimal AND anulado = 0 "
            + "ORDER BY fecha DESC, COALESCE(hora, '') DESC, creado_en DESC")
    public abstract List<Pesaje> historial(String idAnimal);

    @Query("SELECT * FROM pesaje WHERE id_animal = :idAnimal AND anulado = 0 "
            + "ORDER BY fecha DESC, COALESCE(hora, '') DESC, creado_en DESC LIMIT 1")
    public abstract Pesaje ultimo(String idAnimal);
}
