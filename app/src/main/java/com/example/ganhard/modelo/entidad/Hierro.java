package com.example.ganhard.modelo.entidad;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Hierro (marca) del catálogo de la finca. Se registra una vez y se elige en cada animal. No se borra: se desactiva,
 * y deja de ofrecerse para animales nuevos pero se sigue viendo en los que ya lo tienen. El nombre es único por finca
 * sin distinguir mayúsculas.
 */
@Entity(tableName = "hierro",
        foreignKeys = @ForeignKey(entity = Finca.class, parentColumns = "id_finca", childColumns = "id_finca", onDelete = ForeignKey.RESTRICT),
        indices = @Index(value = {"id_finca", "nombre"}, unique = true))
public class Hierro {
    @PrimaryKey @NonNull @ColumnInfo(name = "id_hierro") public String idHierro = "";
    @NonNull @ColumnInfo(name = "id_finca") public String idFinca = "";
    @NonNull @ColumnInfo(collate = ColumnInfo.NOCASE) public String nombre = "";
    public String descripcion;
    public int activo = 1;
    @NonNull @ColumnInfo(name = "creado_en") public String creadoEn = "";
    @NonNull @ColumnInfo(name = "actualizado_en") public String actualizadoEn = "";
    public int version = 0;
}
