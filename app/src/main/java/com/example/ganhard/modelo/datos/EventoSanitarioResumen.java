package com.example.ganhard.modelo.datos;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import com.example.ganhard.modelo.entidad.EventoSanitario;

/** Actividad sanitaria lista para mostrar: el evento, su animal y quién la registró. */
public class EventoSanitarioResumen {
    @Embedded public EventoSanitario evento;
    @ColumnInfo(name = "animal_codigo") public String animalCodigo;
    @ColumnInfo(name = "animal_nombre") public String animalNombre;
    /** Nombre de quien la registró, tomado de la entrada de creación del historial; null si no se conoce. */
    public String autor;
}
