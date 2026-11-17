package com.example.ganhard.modelo.datos;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import com.example.ganhard.modelo.entidad.Tarea;

/** Tarea lista para mostrar: el registro y el animal al que corresponde, si tiene. */
public class TareaResumen {
    @Embedded public Tarea tarea;
    @ColumnInfo(name = "animal_codigo") public String animalCodigo;
    @ColumnInfo(name = "animal_nombre") public String animalNombre;
}
