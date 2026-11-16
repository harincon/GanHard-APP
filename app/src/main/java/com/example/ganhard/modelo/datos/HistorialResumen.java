package com.example.ganhard.modelo.datos;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import com.example.ganhard.modelo.entidad.HistorialCambio;

/** Entrada del historial lista para mostrar: el cambio, quién lo hizo y a qué animal corresponde. */
public class HistorialResumen {
    @Embedded public HistorialCambio cambio;
    public String autor;
    /** Código y nombre del animal afectado, o del animal dueño del pesaje afectado. */
    @ColumnInfo(name = "animal_codigo") public String animalCodigo;
    @ColumnInfo(name = "animal_nombre") public String animalNombre;
}
