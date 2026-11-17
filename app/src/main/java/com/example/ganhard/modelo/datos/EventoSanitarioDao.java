package com.example.ganhard.modelo.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.ganhard.modelo.entidad.EventoSanitario;
import java.util.List;

/** Actividades sanitarias vigentes: las anuladas se conservan en la tabla pero no aparecen en las listas (RN07). */
@Dao
public abstract class EventoSanitarioDao {
    private static final String RESUMEN =
            "SELECT e.*, a.codigo AS animal_codigo, a.nombre AS animal_nombre, "
            + "(SELECT u.nombre FROM historial_cambio h JOIN usuario u ON u.id_usuario = h.id_usuario "
            + "WHERE h.entidad = 'evento_sanitario' AND h.id_registro = e.id_evento_sanitario AND h.accion = 'CREAR' LIMIT 1) AS autor "
            + "FROM evento_sanitario e JOIN animal a ON a.id_animal = e.id_animal WHERE e.anulado = 0 ";
    private static final String ORDEN = "ORDER BY e.fecha DESC, e.creado_en DESC";

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public abstract void insertar(EventoSanitario evento);

    @Update
    public abstract int actualizar(EventoSanitario evento);

    @Query("SELECT * FROM evento_sanitario WHERE id_evento_sanitario = :idEvento")
    public abstract EventoSanitario porId(String idEvento);

    @Query(RESUMEN + "AND a.id_finca = :idFinca " + ORDEN)
    public abstract List<EventoSanitarioResumen> deLaFinca(String idFinca);

    @Query(RESUMEN + "AND e.id_animal = :idAnimal " + ORDEN)
    public abstract List<EventoSanitarioResumen> delAnimal(String idAnimal);
}
