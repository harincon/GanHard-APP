package com.example.ganhard.modelo.repositorio;

import com.example.ganhard.modelo.datos.TareaResumen;
import com.example.ganhard.modelo.entidad.Tarea;

/**
 * Una tarjeta de la lista de tareas: una tarea individual o, juntas, las de un mismo grupo en un mismo estado con su total.
 */
public final class ElementoTarea {
    /** La tarea, si es individual; nulo en un grupo. */
    public final TareaResumen tarea;
    /** El grupo, si lo es; nulo en una tarea individual. */
    public final String idGrupo;
    /** En un grupo, una de sus tareas en ese estado (da tipo, título, fecha y descripción). */
    public final Tarea referencia;
    /** En un grupo, cuántos animales tienen la tarea en ese estado. */
    public final int cantidad;

    private ElementoTarea(TareaResumen tarea, String idGrupo, Tarea referencia, int cantidad) {
        this.tarea = tarea;
        this.idGrupo = idGrupo;
        this.referencia = referencia;
        this.cantidad = cantidad;
    }

    public static ElementoTarea individual(TareaResumen tarea) { return new ElementoTarea(tarea, null, null, 1); }

    static ElementoTarea grupo(String idGrupo, Tarea referencia, int cantidad) { return new ElementoTarea(null, idGrupo, referencia, cantidad); }

    public boolean esGrupo() { return idGrupo != null; }
}
