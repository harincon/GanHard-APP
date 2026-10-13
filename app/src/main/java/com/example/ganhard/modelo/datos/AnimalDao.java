package com.example.ganhard.modelo.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.ganhard.modelo.entidad.Animal;
import java.util.List;

/** Consultas parametrizadas de animales. Crear usa ABORT para que un código repetido falle y no se sobrescriba. */
@Dao
public abstract class AnimalDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    public abstract void insertar(Animal animal);

    /** Devuelve las filas afectadas; 0 significa que el animal no existe. */
    @Update
    public abstract int actualizar(Animal animal);

    @Query("SELECT * FROM animal WHERE id_animal = :idAnimal")
    public abstract Animal porId(String idAnimal);

    @Query("SELECT COUNT(*) FROM animal WHERE id_finca = :idFinca AND estado = 'ACTIVO'")
    public abstract int contarActivos(String idFinca);

    /**
     * Lista de la finca con el último peso vigente por animal en una sola consulta; {@code estado} nulo no filtra.
     * La búsqueda por texto la hace el repositorio, porque SQLite no ignora tildes ni mayúsculas fuera de ASCII.
     */
    @Query("SELECT a.id_animal, a.codigo, a.nombre, a.sexo, a.estado, "
            + "(SELECT p.peso_kg FROM pesaje p WHERE p.id_animal = a.id_animal AND p.anulado = 0 "
            + "ORDER BY p.fecha DESC, COALESCE(p.hora, '') DESC, p.creado_en DESC LIMIT 1) AS ultimo_peso_kg "
            + "FROM animal a WHERE a.id_finca = :idFinca "
            + "AND (:estado IS NULL OR a.estado = :estado) "
            + "ORDER BY a.codigo")
    public abstract List<AnimalResumen> buscar(String idFinca, String estado);
}
