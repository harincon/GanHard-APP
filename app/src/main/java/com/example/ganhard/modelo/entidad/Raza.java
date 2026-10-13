package com.example.ganhard.modelo.entidad;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Catálogo de referencia de razas; no requiere mantenimiento por parte del usuario. */
@Entity(tableName = "raza")
public class Raza {
    @PrimaryKey @NonNull @ColumnInfo(name = "id_raza") public String idRaza = "";
    @NonNull public String nombre = "";
    public String descripcion;
}
