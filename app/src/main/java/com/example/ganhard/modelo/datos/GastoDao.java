package com.example.ganhard.modelo.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.ganhard.modelo.entidad.Gasto;
import java.util.List;

/** Gastos vigentes: los anulados se conservan en la tabla pero no aparecen en listas ni en totales (RN07). */
@Dao
public abstract class GastoDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    public abstract void insertar(Gasto gasto);

    @Update
    public abstract int actualizar(Gasto gasto);

    @Query("SELECT * FROM gasto WHERE id_gasto = :idGasto")
    public abstract Gasto porId(String idGasto);

    /** Del más reciente al más antiguo; {@code desde} es el primer día del periodo y {@code hasta} el primero del siguiente. */
    @Query("SELECT g.*, (SELECT u.nombre FROM historial_cambio h JOIN usuario u ON u.id_usuario = h.id_usuario "
            + "WHERE h.entidad = 'gasto' AND h.id_registro = g.id_gasto AND h.accion = 'CREAR' LIMIT 1) AS autor "
            + "FROM gasto g WHERE g.id_finca = :idFinca AND g.anulado = 0 AND g.fecha >= :desde AND g.fecha < :hasta "
            + "ORDER BY g.fecha DESC, g.creado_en DESC")
    public abstract List<GastoResumen> delPeriodo(String idFinca, String desde, String hasta);

    /** Suma entera en centavos; es 0 si no hay gastos. */
    @Query("SELECT COALESCE(SUM(valor_centavos), 0) FROM gasto WHERE id_finca = :idFinca AND anulado = 0 AND fecha >= :desde AND fecha < :hasta")
    public abstract long total(String idFinca, String desde, String hasta);

    /** Meses (AAAA-MM) con al menos un gasto vigente, del más reciente al más antiguo. */
    @Query("SELECT DISTINCT substr(fecha, 1, 7) FROM gasto WHERE id_finca = :idFinca AND anulado = 0 ORDER BY 1 DESC")
    public abstract List<String> meses(String idFinca);
}
