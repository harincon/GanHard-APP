package com.example.ganhard.modelo.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.ganhard.modelo.entidad.Tarea;
import java.util.List;

/** Tareas de la finca. No se borran: una tarea se cierra completándola o cancelándola. */
@Dao
public abstract class TareaDao {
    /** {@code idAnimal} nulo no filtra por animal. */
    private static final String RESUMEN =
            "SELECT t.*, a.codigo AS animal_codigo, a.nombre AS animal_nombre FROM tarea t "
            + "LEFT JOIN animal a ON a.id_animal = t.id_animal "
            + "WHERE t.id_finca = :idFinca AND (:idAnimal IS NULL OR t.id_animal = :idAnimal) ";

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public abstract void insertar(Tarea tarea);

    @Update
    public abstract int actualizar(Tarea tarea);

    @Query("SELECT * FROM tarea WHERE id_tarea = :idTarea")
    public abstract Tarea porId(String idTarea);

    @Query("SELECT * FROM tarea WHERE id_animal = :idAnimal AND estado = 'PENDIENTE'")
    public abstract List<Tarea> pendientesDelAnimal(String idAnimal);

    /** Todas las tareas del grupo, en cualquier estado. */
    @Query("SELECT * FROM tarea WHERE id_grupo = :idGrupo")
    public abstract List<Tarea> todosDelGrupo(String idGrupo);

    @Query("SELECT * FROM tarea WHERE id_grupo = :idGrupo AND estado = 'PENDIENTE'")
    public abstract List<Tarea> pendientesDelGrupo(String idGrupo);

    /** Las tareas del grupo con su animal, por código de animal. */
    @Query("SELECT t.*, a.codigo AS animal_codigo, a.nombre AS animal_nombre FROM tarea t "
            + "LEFT JOIN animal a ON a.id_animal = t.id_animal WHERE t.id_grupo = :idGrupo ORDER BY a.codigo ASC")
    public abstract List<TareaResumen> delGrupo(String idGrupo);

    /** Los tipos propios que la finca ya usó, para ofrecerlos otra vez. */
    @Query("SELECT DISTINCT etiqueta FROM tarea WHERE id_finca = :idFinca AND etiqueta IS NOT NULL ORDER BY etiqueta COLLATE NOCASE")
    public abstract List<String> etiquetas(String idFinca);

    /** De la fecha prevista más próxima a la más lejana, de modo que las vencidas quedan arriba. */
    @Query(RESUMEN + "AND t.estado = 'PENDIENTE' ORDER BY t.fecha_programada ASC, t.creado_en ASC")
    public abstract List<TareaResumen> pendientes(String idFinca, String idAnimal);

    /** Completadas o canceladas, de la más reciente a la más antigua. */
    @Query(RESUMEN + "AND t.estado = :estado ORDER BY COALESCE(t.fecha_completada, t.fecha_programada) DESC, t.actualizado_en DESC")
    public abstract List<TareaResumen> cerradas(String idFinca, String idAnimal, String estado);
}
