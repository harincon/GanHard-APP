package com.example.ganhard.modelo.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import java.util.List;

/** Entradas del historial, de la más reciente a la más antigua. Solo se insertan y se consultan. */
@Dao
public abstract class HistorialDao {
    /** Une el autor y el animal: directo si el cambio es de un animal, o a través del pesaje, la actividad sanitaria o la tarea afectados. */
    private static final String RESUMEN =
            "SELECT h.*, u.nombre AS autor, a.codigo AS animal_codigo, a.nombre AS animal_nombre "
            + "FROM historial_cambio h "
            + "JOIN usuario u ON u.id_usuario = h.id_usuario "
            + "LEFT JOIN pesaje p ON h.entidad = 'pesaje' AND p.id_pesaje = h.id_registro "
            + "LEFT JOIN evento_sanitario e ON h.entidad = 'evento_sanitario' AND e.id_evento_sanitario = h.id_registro "
            + "LEFT JOIN tarea tt ON h.entidad = 'tarea' AND tt.id_tarea = h.id_registro "
            + "LEFT JOIN animal a ON a.id_animal = CASE WHEN h.entidad = 'animal' THEN h.id_registro ELSE COALESCE(p.id_animal, e.id_animal, tt.id_animal) END ";
    // Entradas del mismo instante: primero la última guardada (si no, el orden sería el de un id al azar).
    private static final String ORDEN = "ORDER BY h.registrado_en DESC, h.rowid DESC";

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public abstract void insertar(HistorialCambio cambio);

    @Query(RESUMEN + "WHERE h.id_finca = :idFinca " + ORDEN)
    public abstract List<HistorialResumen> deLaFinca(String idFinca);

    /** Cambios del animal y de sus pesajes, actividades sanitarias y tareas, incluidos los anulados. */
    @Query(RESUMEN + "WHERE a.id_animal = :idAnimal " + ORDEN)
    public abstract List<HistorialResumen> delAnimal(String idAnimal);
}
