package com.example.ganhard.modelo.entidad;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Actividad sanitaria efectivamente realizada a un animal. Una actividad futura es una tarea, no un evento. */
@Entity(tableName = "evento_sanitario",
        foreignKeys = @ForeignKey(entity = Animal.class, parentColumns = "id_animal", childColumns = "id_animal", onDelete = ForeignKey.RESTRICT),
        indices = @Index(value = {"id_animal", "fecha"}))
public class EventoSanitario {
    public static final String VACUNACION = "VACUNACION", DESPARASITACION = "DESPARASITACION", REVISION = "REVISION",
            TRATAMIENTO = "TRATAMIENTO", OTRO = "OTRO";
    /** Tipos admitidos, en el orden en que se ofrecen. */
    public static final List<String> TIPOS = Collections.unmodifiableList(Arrays.asList(VACUNACION, DESPARASITACION, REVISION, TRATAMIENTO, OTRO));

    @PrimaryKey @NonNull @ColumnInfo(name = "id_evento_sanitario") public String idEventoSanitario = "";
    @NonNull @ColumnInfo(name = "id_animal") public String idAnimal = "";
    @NonNull public String tipo = "";
    @NonNull public String fecha = "";
    @NonNull public String descripcion = "";
    public String producto;
    public String observaciones;
    @ColumnInfo(name = "es_antecedente") public int esAntecedente = 0;
    public int anulado = 0;
    @ColumnInfo(name = "anulado_en") public String anuladoEn;
    @ColumnInfo(name = "motivo_anulacion") public String motivoAnulacion;
    @NonNull @ColumnInfo(name = "creado_en") public String creadoEn = "";
    @NonNull @ColumnInfo(name = "actualizado_en") public String actualizadoEn = "";
    public int version = 0;
}
