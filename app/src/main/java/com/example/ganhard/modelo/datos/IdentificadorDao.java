package com.example.ganhard.modelo.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.ganhard.modelo.entidad.AnimalIdentificador;
import java.util.List;

/** Identificadores de los animales. Aquí no hay unicidad: un mismo valor puede estar en varios animales. */
@Dao
public abstract class IdentificadorDao {
    private static final String RESUMEN =
            "SELECT i.id_animal_identificador, i.id_animal, i.tipo, i.id_hierro, i.valor, i.orden, h.nombre AS hierro_nombre, "
            + "COALESCE(h.activo, 1) AS hierro_activo FROM animal_identificador i LEFT JOIN hierro h ON h.id_hierro = i.id_hierro ";

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public abstract void insertar(AnimalIdentificador identificador);

    @Update
    public abstract void actualizar(AnimalIdentificador identificador);

    @Query("UPDATE animal_identificador SET orden = :orden, actualizado_en = :ahora WHERE id_animal_identificador = :id")
    public abstract void cambiarOrden(String id, int orden, String ahora);

    @Query("DELETE FROM animal_identificador WHERE id_animal_identificador = :id")
    public abstract void eliminar(String id);

    @Query(RESUMEN + "WHERE i.id_animal = :idAnimal ORDER BY i.orden, i.creado_en, i.id_animal_identificador")
    public abstract List<IdentificadorResumen> delAnimal(String idAnimal);

    /** Los identificadores de todos los animales de la finca, para la búsqueda por texto. */
    @Query(RESUMEN + "JOIN animal a ON a.id_animal = i.id_animal WHERE a.id_finca = :idFinca")
    public abstract List<IdentificadorResumen> deLaFinca(String idFinca);
}
