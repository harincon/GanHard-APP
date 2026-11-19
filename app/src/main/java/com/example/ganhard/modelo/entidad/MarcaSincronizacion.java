package com.example.ganhard.modelo.entidad;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Hasta dónde ya se subió una tabla al servidor (respaldo): la fecha y el id del último registro confirmado. Falta
 * por subir todo lo que esté después, ordenado por fecha e id. La fila especial {@link #RESUMEN} guarda cuándo terminó
 * la última sincronización completa.
 */
@Entity(tableName = "marca_sincronizacion")
public class MarcaSincronizacion {
    public static final String RESUMEN = "_resumen";

    @PrimaryKey @NonNull public String tabla = "";
    @NonNull @ColumnInfo(name = "hasta_en") public String hastaEn = "";
    @NonNull @ColumnInfo(name = "hasta_id") public String hastaId = "";
    @NonNull @ColumnInfo(name = "sincronizado_en") public String sincronizadoEn = "";
}
