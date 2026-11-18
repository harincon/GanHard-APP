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

/**
 * Actividad programada, general o de un animal. No es evidencia de una medición ni de una actividad realizadas:
 * al realizarse, el pesaje o la actividad sanitaria guardan su {@code id_tarea} y la tarea queda completada.
 */
@Entity(tableName = "tarea",
        foreignKeys = {
                @ForeignKey(entity = Finca.class, parentColumns = "id_finca", childColumns = "id_finca", onDelete = ForeignKey.RESTRICT),
                @ForeignKey(entity = Animal.class, parentColumns = "id_animal", childColumns = "id_animal", onDelete = ForeignKey.RESTRICT)},
        indices = {
                @Index(value = {"id_finca", "estado", "fecha_programada"}),
                @Index("id_animal"),
                @Index("id_grupo")})
public class Tarea {
    public static final String GENERAL = "GENERAL", PESAJE = "PESAJE", SANIDAD = "SANIDAD";
    public static final String PENDIENTE = "PENDIENTE", COMPLETADA = "COMPLETADA", CANCELADA = "CANCELADA";
    public static final List<String> TIPOS = Collections.unmodifiableList(Arrays.asList(GENERAL, PESAJE, SANIDAD));
    public static final List<String> ESTADOS = Collections.unmodifiableList(Arrays.asList(PENDIENTE, COMPLETADA, CANCELADA));

    @PrimaryKey @NonNull @ColumnInfo(name = "id_tarea") public String idTarea = "";
    @NonNull @ColumnInfo(name = "id_finca") public String idFinca = "";
    /** Obligatorio en las de pesaje y sanidad; opcional en las generales. */
    @ColumnInfo(name = "id_animal") public String idAnimal;
    /** Identifica las tareas programadas juntas para varios animales; nulo en las individuales. */
    @ColumnInfo(name = "id_grupo") public String idGrupo;
    @NonNull public String tipo = GENERAL;
    /** Nombre del tipo cuando la persona escribió uno propio (el tipo queda en GENERAL); nulo en los tipos normales. */
    public String etiqueta;
    @NonNull public String titulo = "";
    public String descripcion;
    @NonNull @ColumnInfo(name = "fecha_programada") public String fechaProgramada = "";
    @NonNull public String estado = PENDIENTE;
    /** Fecha real de finalización; obligatoria cuando la tarea está completada. */
    @ColumnInfo(name = "fecha_completada") public String fechaCompletada;
    @NonNull @ColumnInfo(name = "creado_en") public String creadoEn = "";
    @NonNull @ColumnInfo(name = "actualizado_en") public String actualizadoEn = "";
    public int version = 0;
}
