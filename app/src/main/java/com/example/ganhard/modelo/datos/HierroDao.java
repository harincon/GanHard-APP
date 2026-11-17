package com.example.ganhard.modelo.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.ganhard.modelo.entidad.Hierro;
import java.util.List;

/** Catálogo de hierros de la finca. Crear usa ABORT para que un nombre repetido falle y no se sobrescriba. */
@Dao
public abstract class HierroDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    public abstract void insertar(Hierro hierro);

    @Update
    public abstract int actualizar(Hierro hierro);

    /** Un hierro de la finca por nombre, sin distinguir mayúsculas (la columna es NOCASE); nulo si no hay. */
    @Query("SELECT * FROM hierro WHERE id_finca = :idFinca AND nombre = :nombre LIMIT 1")
    public abstract Hierro porNombre(String idFinca, String nombre);

    @Query("SELECT * FROM hierro WHERE id_hierro = :idHierro")
    public abstract Hierro porId(String idHierro);

    /** Todos los hierros de la finca: primero los activos, y por nombre. */
    @Query("SELECT * FROM hierro WHERE id_finca = :idFinca ORDER BY activo DESC, nombre")
    public abstract List<Hierro> todos(String idFinca);

    @Query("SELECT * FROM hierro WHERE id_finca = :idFinca AND activo = 1 ORDER BY nombre")
    public abstract List<Hierro> activos(String idFinca);
}
