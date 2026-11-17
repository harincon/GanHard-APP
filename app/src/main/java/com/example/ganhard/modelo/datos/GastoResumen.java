package com.example.ganhard.modelo.datos;

import androidx.room.Embedded;
import com.example.ganhard.modelo.entidad.Gasto;

/** Gasto listo para mostrar: el registro y quién lo registró. */
public class GastoResumen {
    @Embedded public Gasto gasto;
    /** Nombre de quien lo registró, tomado de la entrada de creación del historial; null si no se conoce. */
    public String autor;
}
