package com.example.ganhard.modelo.entidad;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "finca", foreignKeys = @ForeignKey(entity = Usuario.class,
        parentColumns = "id_usuario", childColumns = "id_propietario", onDelete = ForeignKey.RESTRICT),
        indices = @Index("id_propietario"))
public class Finca {
    @PrimaryKey @NonNull @ColumnInfo(name = "id_finca") public String idFinca = "";
    @NonNull @ColumnInfo(name = "id_propietario") public String idPropietario = "";
    @NonNull public String nombre = "";
    public String ubicacion;
    @ColumnInfo(name = "area_hectareas") public Double areaHectareas;
    public int activa = 1;
    /** Instante (UTC) en que la finca se vinculó a una cuenta; nulo si solo existe en este teléfono. */
    @ColumnInfo(name = "vinculada_en") public String vinculadaEn;
    @NonNull @ColumnInfo(name = "creado_en") public String creadoEn = "";
    @NonNull @ColumnInfo(name = "actualizado_en") public String actualizadoEn = "";
    public int version = 0;
}
