package com.example.ganhard.modelo.entidad;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Medición efectivamente realizada. Una planificación futura es una tarea, no un pesaje. */
@Entity(tableName = "pesaje",
        foreignKeys = @ForeignKey(entity = Animal.class, parentColumns = "id_animal", childColumns = "id_animal", onDelete = ForeignKey.RESTRICT),
        indices = @Index(value = {"id_animal", "fecha", "hora"}))
public class Pesaje {
    @PrimaryKey @NonNull @ColumnInfo(name = "id_pesaje") public String idPesaje = "";
    @NonNull @ColumnInfo(name = "id_animal") public String idAnimal = "";
    @NonNull public String fecha = "";
    public String hora;
    @ColumnInfo(name = "peso_kg") public double pesoKg;
    @ColumnInfo(name = "es_antecedente") public int esAntecedente = 0;
    public String observaciones;
    public int anulado = 0;
    @ColumnInfo(name = "anulado_en") public String anuladoEn;
    @ColumnInfo(name = "motivo_anulacion") public String motivoAnulacion;
    @NonNull @ColumnInfo(name = "creado_en") public String creadoEn = "";
    @NonNull @ColumnInfo(name = "actualizado_en") public String actualizadoEn = "";
    public int version = 0;
}
