package com.example.ganhard.modelo.repositorio;

import com.example.ganhard.modelo.datos.EventoSanitarioResumen;
import com.example.ganhard.modelo.entidad.EventoSanitario;

/**
 * Una tarjeta de la lista general de sanidad: una actividad individual o, juntas, las de un mismo grupo con su total.
 */
public final class ElementoSanidad {
    /** La actividad, si es individual; nulo en un grupo. */
    public final EventoSanitarioResumen actividad;
    /** El grupo, si lo es; nulo en una actividad individual. */
    public final String idGrupo;
    /** En un grupo, una de sus actividades vigentes (da tipo, fecha, producto y descripción). */
    public final EventoSanitario referencia;
    /** En un grupo, cuántos animales tienen la actividad vigente. */
    public final int cantidad;

    private ElementoSanidad(EventoSanitarioResumen actividad, String idGrupo, EventoSanitario referencia, int cantidad) {
        this.actividad = actividad;
        this.idGrupo = idGrupo;
        this.referencia = referencia;
        this.cantidad = cantidad;
    }

    public static ElementoSanidad individual(EventoSanitarioResumen actividad) {
        return new ElementoSanidad(actividad, null, null, 1);
    }

    static ElementoSanidad grupo(String idGrupo, EventoSanitario referencia, int cantidad) {
        return new ElementoSanidad(null, idGrupo, referencia, cantidad);
    }

    public boolean esGrupo() { return idGrupo != null; }
}
